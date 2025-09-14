## How to participate in the p2p Infra DAO/Loadbalancer for Ergo Explorer

Prerequisites:
- Ubuntu Server (preferred)
- Install Nginx
- Install Docker
- git
- Fully Synced AND INDEXED Ergo Node running on the same machine (accessible at 127.0.0.1:9053, or change config files in explorer for remote node)
- Forward the following ports on your router from external to internal: 443, 3000, 3001, 8080, 9053
  
YOU HAVE TO HAVE THE ABOVE DONE 100% before proceeding. A node can take a day or more the fully sync, the below Explorer can take a few days to sync on top of that even.

## 🐧 Cloning This Repository on Ubuntu

Open your terminal and run the following command:

```
git clone https://github.com/andrehafner/p2p-explorer.git
```
This will download the repository into a folder named p2p-explorer. To navigate into it:
```
cd p2p-explorer
```
Typically, there are three passwords you can change (or just don't touch anything). When this is firt built on your system, it takes these passwords and uses them, any changes to these files after that initial build and you would need to rebuild the database or change the password on the database manually).
p2p-explorer/explorer-backend-9.17.4/docker-compose.yaml has a postgres password and IP change
p2p-explorer/db/db.secret has a db and a postgres password
*I need to see which is actually used for now, in the sample i have two different ones, so, one must not be utilized*

You can edit those files in the terminal with nano
Example
```
sudo nano p2p-explorer/explorer-backend-9.17.4/docker-compose.yaml
sudo nano p2p-explorer/db/db.secret
```
if you do edit, after editing, press ctrl+o and then enter to save, then ctrl+x to exit

Since we are load balancing, we need the front end to rely on the local database, if we put the load balanced DNS, it will NOT be doing that, so we have to put the external IP address of this machine. This means you need to OPEN port 8080 and forward it to this machine (ping us in chat if you need help).
Next, open the follwing file and find this line "API: http://yourexternalIP:8080" and put your external IP address where it says 'yourexternalIP': 
```
sudo nano p2p-explorer/docker-compose.yml
```
after editing, press ctrl+o and then enter to save, then ctrl+x to exit

We first need to create two things for docker:
type the following commands into the terminal pressing enter after each one
```
sudo docker network create ergo-node
sudo docker volume create --name-ergo_redis
```

Let's build, make sure you are in the main p2p-explorer folder!
```
sudo docker build
```
This can take some time (10 minutes)
If you chose a minimal Ubuntu server install, you will probably be missing some libraries and the build will fail. Look at the terminal where it fails, copy that to chatgpt and ask how to install that library, repeat the build command and rinse and repeat. 

Once built with no errors, start it up (same directory)!
```
sudo docker compose up -d
```
The -d here starts it in detached mode, sometimes it's helpful to start it the first time with just 'sudo docker compose up' and then you can watch the logs. Press ctrl+c to stop it if you do this and redo it with -d so you can exit your terminal without it quitting the explorer.

If you need to stop it
```
sudo docker compose down
```

You should now be able to access your explorer at the following http://yourIPaddress:3000

Next:
If you do not want to be part of the load balance, you can simply download python/certbot and get a cert that will automatically configure NGINX, after that, replace all intances of the below NGINX file where it says the p2p domain to your domain, and fix the docker-compose.yml url above (no need for port 3000 if you are using NGINX properly and a domain name.

If you want to load balance, open the following file:
```
sudo nano /etc/nginx/sites-enabled/default
```
Erase the contents, and paste in the following:
```
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
Lastly, we need the .pem and .key files for the https domain name to work
Reach out to the Ergo Infra DAO to become a member to receive these files: [PAIDEIA](https://app.paideia.im/ergoinfradao)

Once obtained, open them in a formatless text editor (link notpad++ pr sublime text) and run the following commands:
Create directories as needed
```
sudo mkdir -p /etc/ssl/cloudflare/
```
Now, run each command, paste in the contents of the file, and ctrl+o and enter to save, ctrl+x to exit each time
```
sudo nano /etc/ssl/cloudflare/ergoplatform-p2p.pem
sudo nano /etc/ssl/cloudflare/ergoplatform-p2p.key
sudo nano /etc/ssl/cloudflare/origin_ca_rsa_root.pem
```
Once done, we test the NGINX file 
Read the output here, it will tell you what's wrong if there is an issue
```
sudo nginx -t
```
Reload and restart
```
sudo systemctl reload nginx
sudo systemctl restart nginx
```

Contact qx for coordination to the load balancer, you will need to do the following now that you are all synced (you can do this now while you wait): 
1 - Open the follwing file again and find this line "API: http://yourexternalIP:8080" and replace it this time with https://api-p2p.ergoplatform.com: 
```
sudo nano p2p-explorer/docker-compose.yml
```
after editing, press ctrl+o and then enter to save, then ctrl+x to exit 
Now you need to rebuild the UI with the following command so it uses the load balanced api: 
```
sudo docker compose build --no-cache ui
```

You should now be able to reach the explorer at p2p-explorer.ergoplatform.com
