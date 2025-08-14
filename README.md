
## How to Participate in the p2p Infra DAO/Load Balancer for Ergo Explorer

### Prerequisites:
- Ubuntu Server (preferred)
- Install Nginx
- Install Docker
- Git
- Fully Synced AND INDEXED Ergo Node running on the same machine (accessible at 127.0.0.1:9053, or change config files in explorer for a remote node)

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

Typically, there are three passwords you can change (or leave them as they are). When this is first built on your system, it takes these passwords and uses them. Any changes to these files after the initial build will require a database rebuild or manual password change.

- `p2p-explorer/explorer-backend-9.17.4/docker-compose.yaml` has a PostgreSQL password and IP change.
- `p2p-explorer/db/db.secret` has a database and a PostgreSQL password.
  *I need to see which is actually used for now, in the sample, I have two different ones, so one must not be utilized.*

You can edit these files in the terminal with `nano`. For example:

```sh
sudo nano p2p-explorer/explorer-backend-9.17.4/docker-compose.yaml
sudo nano p2p-explorer/db/db.secret
```

If you do edit, after editing, press `Ctrl+O` and then `Enter` to save, then `Ctrl+X` to exit.

Since we are load balancing, the front end must rely on the local database. If we use the load-balanced DNS, it will not do that, so we must use the external IP address of this machine. This means you need to open port 8080 and forward it to this machine (ping us in chat if you need help).

Next, open the following file and find this line `"API: http://yourexternalIP:8080"` and replace `'yourexternalIP'` with your external IP address:

```sh
sudo nano p2p-explorer/docker-compose.yml
```

After editing, press `Ctrl+O` and then `Enter` to save, then `Ctrl+X` to exit.

We first need to create two things for Docker:

```sh
sudo docker network create ergo-node
sudo docker volume create --name ergo_redis
```

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

### Next Steps:

If you do not want to be part of the load balance, you can simply download Python/Certbot and get a cert that will automatically configure Nginx. After that, replace all instances of the below Nginx file where it says the p2p domain with your domain, and fix the `docker-compose.yml` URL above (no need for port 3000 if you are using Nginx properly and a domain name).

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