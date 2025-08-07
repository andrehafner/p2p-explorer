package org.ergoplatform.explorer.db.queries

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.refined.implicits._
import doobie.Fragments.in
import doobie.LogHandler
import doobie.util.fragment.Fragment
import doobie.util.query.Query0
import org.ergoplatform.explorer.{BoxId, HexString, TxId}
import org.ergoplatform.explorer.db.models.UInput
import org.ergoplatform.explorer.db.models.aggregates.ExtendedUInput

object UInputQuerySet extends QuerySet {

  import org.ergoplatform.explorer.db.doobieInstances._

  val tableName: String = "node_u_inputs"

  val fields: List[String] = List(
    "box_id",
    "tx_id",
    "index",
    "proof_bytes",
    "extension"
  )

  def getAll(offset: Int, limit: Int)(implicit lh: LogHandler): Query0[ExtendedUInput] =
    sql"""
         |select distinct on (i.box_id, i.tx_id)
         |  i.box_id,
         |  i.tx_id,
         |  i.index,
         |  i.proof_bytes,
         |  i.extension,
         |  case when (o.value is null)     then ou.value                else o.value end,
         |  case when (o.tx_id is null)     then ou.tx_id                else o.tx_id end,
         |  case when (o.header_id is null) then null                    else o.header_id end,
         |  case when (o.index is null)     then ou.index                else o.index end,
         |  case when (o.ergo_tree is null) then ou.ergo_tree            else o.ergo_tree end,
         |  case when (o.address is null)   then ou.address              else o.address end,
         |  case when (o.box_id is null)    then ou.additional_registers else o.additional_registers end
         |from node_u_inputs i
         |left join node_outputs o on i.box_id = o.box_id
         |left join node_u_outputs ou on i.box_id = ou.box_id
         |where o.box_id is not null or ou.box_id is not null
         |offset $offset limit $limit
         |""".stripMargin.query[ExtendedUInput]

  def getAllByTxId(txId: TxId)(implicit lh: LogHandler): Query0[ExtendedUInput] =
    sql"""
         |select distinct on (i.box_id, i.index)
         |  i.box_id,
         |  i.tx_id,
         |  i.index,
         |  i.proof_bytes,
         |  i.extension,
         |  case when (o.value is null)     then ou.value                else o.value end,
         |  case when (o.tx_id is null)     then ou.tx_id                else o.tx_id end,
         |  case when (o.header_id is null) then null                    else o.header_id end,
         |  case when (o.index is null)     then ou.index                else o.index end,
         |  case when (o.ergo_tree is null) then ou.ergo_tree            else o.ergo_tree end,
         |  case when (o.address is null)   then ou.address              else o.address end,
         |  case when (o.box_id is null)    then ou.additional_registers else o.additional_registers end
         |from node_u_inputs i
         |left join node_outputs o on i.box_id = o.box_id
         |left join node_u_outputs ou on i.box_id = ou.box_id
         |where i.tx_id = $txId and (o.box_id is not null or ou.box_id is not null)
         |order by i.index asc
         |""".stripMargin.query[ExtendedUInput]

  def getAllByTxIds(txIds: NonEmptyList[TxId])(implicit lh: LogHandler): Query0[ExtendedUInput] = {
    val queryFr =
      fr"""
          |select distinct on (i.box_id, i.tx_id)
          |  i.box_id,
          |  i.tx_id,
          |  i.index,
          |  i.proof_bytes,
          |  i.extension,
          |  case when (o.value is null)     then ou.value                else o.value end,
          |  case when (o.tx_id is null)     then ou.tx_id                else o.tx_id end,
          |  case when (o.header_id is null) then null                    else o.header_id end,
          |  case when (o.index is null)     then ou.index                else o.index end,
          |  case when (o.ergo_tree is null) then ou.ergo_tree            else o.ergo_tree end,
          |  case when (o.address is null)   then ou.address              else o.address end,
          |  case when (o.box_id is null)    then ou.additional_registers else o.additional_registers end
          |from node_u_inputs i
          |left join node_outputs o on i.box_id = o.box_id
          |left join node_u_outputs ou on i.box_id = ou.box_id
          |where (o.box_id is not null or ou.box_id is not null) and i.tx_id
          |""".stripMargin
    in(queryFr, txIds).query[ExtendedUInput]
  }

  def getAllUInputBoxIdsByErgoTree(ergoTree: HexString): Query0[BoxId] =
    fr"""
        |select distinct on (i.box_id, i.tx_id)
        |  i.box_id
        |from node_u_inputs i
        |left join node_outputs o on i.box_id = o.box_id
        |left join node_u_outputs ou on i.box_id = ou.box_id
        |where (o.box_id is not null or ou.box_id is not null) and i.tx_id in
        | ( select distinct on (t.id) t.id from node_u_transactions t
        |    left join node_u_inputs ui on ui.tx_id = t.id
        |    left join node_u_outputs uo on uo.tx_id = t.id
        |    left join node_outputs o on o.box_id = ui.box_id
        |    where uo.ergo_tree = $ergoTree or o.ergo_tree = $ergoTree)    
        |""".stripMargin
      .query[BoxId]
}
