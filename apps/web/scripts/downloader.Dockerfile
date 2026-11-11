FROM alpine:latest

# Install wget and tar
RUN apk add --no-cache wget tar

WORKDIR /client

# Keep a shell by default
CMD ["sh"]
