# How to Participate in the p2p Infra DAO/Load Balancer for Ergo Explorer

## Prerequisites
- Ubuntu Server (preferred)
- Nginx installed
- Docker installed
- Git installed
- Fully synced AND indexed Ergo node running on the same machine (accessible at 127.0.0.1:9053, or change config files in explorer for a remote node)

**Note:** Ensure that all the above prerequisites are fully completed before proceeding. Syncing a node can take a day or more, and the Explorer can take a few additional days to sync on top of that.

## 🐧 Cloning This Repository on Ubuntu

Open your terminal and run the following command:

```sh
git clone https://github.com/andrehafner/p2p-explorer.git
```

This will download the repository into a folder named `p2p-explorer`. To navigate into it:

```sh
cd p2p-explorer
```

## 🔐 Configuration Files

There are typically three passwords you can change (or leave them as they are). When this is first built on your system, it takes these passwords and uses them. Any changes to these files after the initial build will require a database rebuild or manual password change.

- `p2p-explorer/explorer-backend-9.17.4/docker-compose.yaml` contains a PostgreSQL password and IP change.
- `p2p-explorer/db/db.secret` contains database and PostgreSQL passwords.

You can edit these files in the terminal with `nano`. For example:

```sh
sudo nano p2p-explorer/explorer-backend-9.17.4/docker-compose.yaml
sudo nano p2p-explorer/db/db.secret
```

If you do edit, after editing, press `Ctrl+O` and then `Enter` to save, then `Ctrl+X` to exit.

## 🌐 External IP Configuration

Since we are load balancing, the frontend must rely on the local database. If we use the load-balanced DNS, it will not do that, so we must use the external IP address of this machine. This means you need to open port 8080 and forward it to this machine (ping us in chat if you need help).

Next, open the following file and find this line `"API: http://yourexternalIP:8080"` and replace `'yourexternalIP'` with your external IP address:

```sh
sudo nano p2p-explorer/docker-compose.yml
```

After editing, press `Ctrl+O` and then `Enter` to save, then `Ctrl+X` to exit.

## 🐳 Docker Setup

We first need to create two things for Docker:

```sh
sudo docker network create ergo-node
sudo docker volume create --name ergo_redis
```

Next, make sure there is a postgres_data directory with appropriate permissions:

```sh
mkdir -p postgres_data
sudo chown 999:999 postgres_data
```

## 🚀 Building and Running the Project

Let's build the project. Make sure you are in the main `p2p-explorer` folder!

```sh
sudo docker compose up --build
```

This can take some time (approximately 10 minutes). If you chose a minimal Ubuntu server install, you will probably be missing some libraries, and the build will fail. Look at the terminal where it fails, copy that error to a chat, and ask how to install the missing library. Repeat the build command and rinse and repeat.

Once built with no errors, start it up (same directory):

```sh
sudo docker compose up -d
```

The `-d` flag starts it in detached mode. Sometimes it's helpful to start it the first time with just `sudo docker compose up` so you can watch the logs. Press `Ctrl+C` to stop it if you do this and redo it with `-d` so you can exit your terminal without quitting the explorer.

If you need to stop it:

```sh
sudo docker compose down
```

You should now be able to access your explorer at the following URL: `http://yourIPaddress:3000`.

## 🔄 Next Steps

### Option 1: Standalone Setup (No Load Balancing)

If you do not want to be part of the load balancer, you can simply download Python/Certbot and get a cert that will automatically configure Nginx. After that, replace all instances of the below Nginx file where it says the p2p domain with your domain, and fix the `docker-compose.yml` URL above (no need for port 3000 if you are using Nginx properly and a domain name).

### Option 2: Load Balancing Setup

If you want to load balance, open the following file:

```sh
sudo nano /etc/nginx/sites-enabled/default
```

Erase the contents and paste in the following:

```nginx
server {
    listen 443 ssl http2;
    server_name explorer-p2p.ergoplatform.com;
    ssl_certificate     /etc/ssl/cloudflare/ergoplatform-p2p.pem;
    ssl_certificate_key /etc/ssl/cloudflare/ergoplatform-p2p.key;
    ssl_trusted_certificate /etc/ssl/cloudflare/origin_ca_rsa_root.pem;

    location / { proxy_pass http://localhost:3000; }
}

server {
    listen 443 ssl http2;
    server_name api-p2p.ergoplatform.com;
    ssl_certificate     /etc/ssl/cloudflare/ergoplatform-p2p.pem;
    ssl_certificate_key /etc/ssl/cloudflare/ergoplatform-p2p.key;
    ssl_trusted_certificate /etc/ssl/cloudflare/origin_ca_rsa_root.pem;

    location / {
        proxy_pass http://localhost:8080;
        rewrite ^(?!/api/v[0-9])(/.*)$ /api/v0$1 break;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 443 ssl http2;
    server_name graphql-p2p.ergoplatform.com;
    ssl_certificate     /etc/ssl/cloudflare/ergoplatform-p2p.pem;
    ssl_certificate_key /etc/ssl/cloudflare/ergoplatform-p2p.key;
    ssl_trusted_certificate /etc/ssl/cloudflare/origin_ca_rsa_root.pem;

    location / { proxy_pass http://localhost:3001; }
}

server {
    listen 443 ssl http2;
    server_name node-p2p.ergoplatform.com;
    ssl_certificate     /etc/ssl/cloudflare/ergoplatform-p2p.pem;
    ssl_certificate_key /etc/ssl/cloudflare/ergoplatform-p2p.key;
    ssl_trusted_certificate /etc/ssl/cloudflare/origin_ca_rsa_root.pem;

    location / { proxy_pass http://localhost:9053; }
}
```

## 🔒 SSL Certificate Setup

Lastly, we need the `.pem` and `.key` files for the HTTPS domain name to work. Reach out to the Ergo Infra DAO to become a member to receive these files: [PAIDEIA](https://app.paideia.im/ergoinfradao).

Once obtained, open them in a formatless text editor (like Notepad++ or Sublime Text) and run the following commands:

Create directories as needed:

```sh
sudo mkdir -p /etc/ssl/cloudflare/
```

Now, run each command, paste in the contents of the file, and press `Ctrl+O` and `Enter` to save, then `Ctrl+X` to exit each time:

```sh
sudo nano /etc/ssl/cloudflare/ergoplatform-p2p.pem
sudo nano /etc/ssl/cloudflare/ergoplatform-p2p.key
sudo nano /etc/ssl/cloudflare/origin_ca_rsa_root.pem
```

Once done, test the Nginx file:

```sh
sudo nginx -t
```

Read the output here; it will tell you what's wrong if there is an issue.

Reload and restart:

```sh
sudo systemctl reload nginx
sudo systemctl restart nginx
```

You should now be able to reach the explorer at `yourdomain.whatever` or, if you are part of the load balancer, `p2p-explorer.ergoplatform.com`.

## 🏠 Connecting to an Ergo Node on Another Machine in Your Home Network

If you want to run the Ergo Explorer on one machine but connect to an Ergo node running on a different machine within your home network (useful for splitting storage requirements or using a dedicated node machine), you'll need to update two configuration files.

### Prerequisites
- Ensure your Ergo node machine is accessible from the machine running the Explorer
- Verify the Ergo node is running and accessible on port 9053
- Know the local IP address of your Ergo node machine (e.g., `192.168.1.100`)

### Step 1: Update docker-compose.yml
Edit the GraphQL service configuration in `docker-compose.yml`:

```sh
sudo nano docker-compose.yml
```

Find this line in the GraphQL service section:
```yaml
ERGO_NODE_ADDRESS: http://172.17.0.1:9053
```

Change it to your Ergo node machine's IP address:
```yaml
ERGO_NODE_ADDRESS: http://192.168.1.100:9053
```

### Step 2: Update explorer-backend.conf
Edit the backend configuration file:

```sh
sudo nano explorer-backend.conf
```

Find this line:
```
master-nodes = ["http://172.17.0.1:9053"]
```

Change it to your Ergo node machine's IP address:
```
master-nodes = ["http://192.168.1.100:9053"]
```

### Step 3: Update Nginx Configuration
If you're using Nginx with SSL/domain setup, you'll also need to update the Nginx configuration to point to your remote Ergo node instead of localhost.

Edit the Nginx configuration file:

```sh
sudo nano /etc/nginx/sites-enabled/default
```

Find the server block for your node domain (e.g., `node-p2p.ergoplatform.com`) and change this line:

```nginx
location / { proxy_pass http://localhost:9053; }
```

To point to your Ergo node machine's IP address:

```nginx
location / { proxy_pass http://192.168.1.100:9053; }
```

After updating Nginx, test and reload the configuration:

```sh
sudo nginx -t
sudo systemctl reload nginx
```

### Step 4: Restart Services
After making all changes, restart all services:

```sh
sudo docker compose down
sudo docker compose up -d
```

### Why Two Updates Are Needed
- **GraphQL service**: Reads from `docker-compose.yml` environment variables
- **Backend services** (grabber, utx-tracker, api): Read from `explorer-backend.conf`
- Both must point to the same Ergo node address for proper functionality

### Troubleshooting
- Test connectivity: `ping <YOUR_NODE_IP>`
- Verify port access: `telnet <YOUR_NODE_IP> 9053`
- Check firewall settings on both machines
- Ensure both machines are on the same network segment

## 🔌 Port Forwarding Requirements

When running the Ergo node and p2p-explorer on different machines, you'll need to configure port forwarding on your router/gateway for proper connectivity.

### Ergo Node Machine Ports
- **Port 9053** - Main Ergo node API (required for explorer connectivity)
- **Port 9030** - P2P network communication (required for node syncing)

### P2P-Explorer Machine Ports
- **Port 443** - HTTPS access (if using SSL/domain)
- **Port 3000** - Frontend UI access
- **Port 3001** - GraphQL service access
- **Port 8080** - Backend API access

### How to Configure Port Forwarding
1. **Access your router/gateway** (e.g., Xfinity Gateway app, router admin panel)
2. **Navigate to Port Forwarding section** (may be called "Port Forwarding," "Port Mapping," or "Virtual Server")
3. **Add rules for each port**:
   - **External Port**: Same as internal port (e.g., 9053 → 9053)
   - **Internal IP**: The local IP address of the target machine
   - **Protocol**: TCP (or TCP/UDP for 9030)
4. **Save and apply** the port forwarding rules

**Note**: The exact steps vary by router manufacturer. For Xfinity Gateway users, use the Xfinity app to configure port forwarding as it's often easier than the web interface.

**Security Consideration**: Only forward the ports you actually need. Ports 9053 and 9030 should only be accessible from your local network, while the explorer ports (3000, 3001, 8080, 443) may need external access depending on your setup.

## 🏗️ System Architecture & Functionality Overview

The p2p-explorer is a comprehensive blockchain exploration system built with a microservices architecture. Here's how all the components work together:

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              USER INTERFACE LAYER                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Frontend UI (Port 3000)  │  GraphQL Service (Port 3001)  │  Nginx (Port 443)   │
│  • Web interface          │  • Query language for data    │  • SSL termination  │
│  • User interactions      │  • Real-time subscriptions    │  • Load balancing   │
│  • Responsive design      │  • Efficient data fetching    │  • Domain routing   │
└───────────────────────────┼────────────────────────────────┼───────────────────-┘
                            │                                │
                            ▼                                ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              API & SERVICES LAYER                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Backend API (Port 8080)  │  Chain Grabber  │  UTX Tracker  │  UTX Broadcaster  │
│  • REST API endpoints     │  • Block sync   │  • Mempool    │  • Transaction    │
│  • Business logic         │  • Indexing     │  • UTXO       │  • broadcasting   │
│  • Data processing        │  • Validation   │  • Monitoring │  • Network        │
└───────────────────────────┼─────────────────┼───────────────┼───────────────────┘
                            │                 │               │
                            ▼                 ▼               ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              DATA & STORAGE LAYER                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│  PostgreSQL Database     │  Redis Cache    │  Redis Request Cache               │
│  (Port 5433)             │  (Port 6379)    │  (Internal)                        │
│  • Blockchain data       │  • API responses│  • Query caching                   │
│  • Transaction history   │  • Sessions     │  • Performance optimization        │
│  • Address balances      │  • Real-time    │  • Reduced database load           │
└───────────────────────────┼─────────────────┼───────────────────────────────────┘
                            │                 │
                            ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              BLOCKCHAIN LAYER                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Ergo Node (Port 9053)   │  P2P Network (Port 9030)                             │
│  • Full blockchain sync  │  • Peer discovery                                    │
│  • Transaction pool      │  • Block propagation                                 │
│  • State management      │  • Network consensus                                 │
│  • API endpoints         │  • Decentralized communication                       │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Component Functions

#### **Frontend UI (Port 3000)**
- **Purpose**: User-friendly web interface for blockchain exploration
- **Features**: Block browser, transaction lookup, address search, network stats
- **Technology**: Modern JavaScript framework with responsive design

#### **GraphQL Service (Port 3001)**
- **Purpose**: Efficient data querying and real-time updates
- **Features**: Single endpoint, flexible queries, subscription support
- **Benefits**: Reduced over-fetching, optimized data retrieval

#### **Backend API (Port 8080)**
- **Purpose**: Core business logic and data processing
- **Features**: REST endpoints, data validation, service coordination
- **Responsibilities**: Block processing, transaction analysis, address management

#### **Chain Grabber**
- **Purpose**: Continuous blockchain synchronization and indexing
- **Process**: Monitors node → Fetches new blocks → Processes data → Updates database
- **Output**: Indexed blockchain data for fast queries

#### **UTX Tracker**
- **Purpose**: Monitor unspent transaction outputs (UTXOs)
- **Process**: Tracks mempool changes → Updates UTXO state → Maintains consistency
- **Output**: Real-time UTXO status and balance updates

#### **UTX Broadcaster**
- **Purpose**: Broadcast transactions to the Ergo network
- **Process**: Receives transactions → Validates → Sends to node → Updates status
- **Output**: Transaction propagation confirmation

#### **PostgreSQL Database (Port 5433)**
- **Purpose**: Persistent storage for all blockchain data
- **Features**: ACID compliance, complex queries, indexing, partitioning
- **Data**: Blocks, transactions, addresses, balances, network statistics

#### **Redis Cache (Port 6379)**
- **Purpose**: High-performance caching layer
- **Features**: API response caching, session management, real-time data
- **Benefits**: Faster response times, reduced database load

#### **Ergo Node (Port 9053)**
- **Purpose**: Source of truth for blockchain data
- **Features**: Full blockchain sync, transaction pool, state management
- **Requirements**: Significant storage space, continuous internet connection

### Data Flow

1. **Blockchain Updates**: New blocks arrive at Ergo Node
2. **Data Processing**: Chain Grabber fetches and processes new data
3. **Storage**: Processed data is stored in PostgreSQL with Redis caching
4. **API Access**: Backend API provides structured access to data
5. **Frontend Delivery**: GraphQL and REST APIs serve data to frontend
6. **User Experience**: Frontend presents data in user-friendly format

### Network Architecture

- **Internal Communication**: Services communicate via Docker network
- **External Access**: Port forwarding enables remote access
- **Load Balancing**: Nginx distributes traffic across services
- **Security**: SSL termination and proper port management

### Performance Characteristics

- **Scalability**: Microservices can be scaled independently
- **Reliability**: Redundant services with failover capabilities
- **Efficiency**: Optimized data storage and retrieval patterns
- **Monitoring**: Built-in health checks and logging

This architecture ensures a robust, scalable, and maintainable blockchain exploration system that can handle the demands of a production environment while providing an excellent user experience.
