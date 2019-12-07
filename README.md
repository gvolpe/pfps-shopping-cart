shopping-cart
=============

![CI Status](https://github.com/gvolpe/pfps-shopping-cart/workflows/Build/badge.svg)
[![MergifyStatus](https://img.shields.io/endpoint.svg?url=https://gh.mergify.io/badges/gvolpe/pfps-shopping-cart&style=flat)](https://mergify.io)

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

We need to run both `PostgreSQL` and `Redis`. Get started quickly using `docker-compose`:

```
docker-compose up
```

## Tests

To run Unit Tests:

```
sbt test
```

To run Integration Tests:

```
docker-compose up
sbt it:test
docker-compose down
```

## Deploying

Set the proper environment variable (`test` or `prod`):

```
export SC_APP_ENV="test"
```

### Build Docker image

```
sbt docker:publishLocal
```

Our image should now be built. We can check it by running the following command:

```
> docker images | grep shopping-cart
REPOSITORY                    TAG                 IMAGE ID            CREATED              SIZE
shopping-cart                 latest              646501a87362        2 seconds ago       138MB
```

To run our application using our Docker image, run the following command:

```
cd /app
docker-compose up
```

## HTTP API Resources

If you use the [Insomnia](https://insomnia.rest/) REST Client, you can import the shopping cart resources using the [insomnia.json](insomnia.json) file.

## LICENSE

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with
the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.
