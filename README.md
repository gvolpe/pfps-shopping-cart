shopping-cart
=============

### Authentication Data

For didactic purposes this information is made available to the readers but in a real application *THIS SHOULD NEVER BE MADE PUBLIC*.

For Admin users:

- `export SC_JWT_SECRET_KEY="-*5h0pp1ng_k4rt*-"`
- `export SC_JWT_CLAIM="{004b4457-71c3-4439-a1b2-03820263b59c}"`
- `export SC_ADMIN_USER_TOKEN="eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.ezA0YjQ0NTctNzFjMy00NDM5LWExYjItMDM4MjAyNjNiNTl9.mMC4eiPl7huiAO3GdORwKnqJrf96xKPoojQdZtrTbP4"`

For access token (manipulation of the shopping cart):

- `export SC_ACCESS_TOKEN_SECRET_KEY="5h0pp1ng_k4rt"`

For password encryption:

- `export SC_PASSWORD_SALT="06!grsnxXG0d*Pj496p6fuA*o"`

## Requirements

We need to run both `PostgreSQL` and `Redis` in order to make our application work. Get started quickly using Docker.

### PostgreSQL

```
docker run --rm --name psql -e POSTGRES_DB=store -p 5432:5432 -v $HOME/docker/volumes/postgres:/var/lib/postgresql/data postgres:11.1-alpine
```

#### Connect using PSQL

```
psql -h localhost -U postgres
```

### Redis

```
docker run --rm --name cache -p 6379:6379 redis:5.0.0
```

## Deploying

Set the proper environment variable ("test" or "prod"):

```
export SC_APP_ENV="test"
```

## HTTP API Resources

If you use the [Insomnia](https://insomnia.rest/) REST Client, you can import the shopping cart resources using the [insomnia.json](insomnia.json) file.
