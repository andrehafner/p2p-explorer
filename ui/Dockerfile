FROM --platform=linux/arm64 node:20-bullseye as build
ENV NODE_OPTIONS=--openssl-legacy-provider
RUN apt-get update && apt-get upgrade -y && apt-get install -y curl python3 python3-pip make g++ python3-setuptools python3-distutils
# npx browserslist@latest --update-db
RUN curl -L https://github.com/ergoplatform/explorer-frontend/archive/refs/heads/master.tar.gz > /tmp/src.tar.gz && \
    tar -xf /tmp/src.tar.gz  -C /tmp && \
    mv /tmp/explorer-frontend-master /app
WORKDIR /app
ARG API
ARG LABEL
COPY environment.default.ts /app/src/config/
COPY environment.prod.ts /app/src/config/
# Set configured api in config templates
RUN sed -i "s|_api_|${API}|g" /app/src/config/environment.default.ts && \
    sed -i "s|_api_|${API}|g" /app/src/config/environment.prod.ts && \
    # Set configured network name in config templates   
    sed -i "s|_name_|${LABEL}|g" /app/src/config/environment.default.ts && \
    sed -i "s|_name_|${LABEL}|g" /app/src/config/environment.prod.ts
# Build
RUN npm i --location=global corepack && \
    yarn && \
    yarn install && \
    yarn run build && \
    yarn cache clean && \
    yarn global add serve
RUN npx browserslist@latest --update-db
FROM --platform=linux/arm64 node:20-bullseye
ENV NODE_ENV production
WORKDIR /app
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/build ./build
RUN npm i --location=global corepack && \
    yarn global add serve
CMD ["serve", "-s", "build"]
EXPOSE 3000
