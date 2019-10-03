shopping-cart
=============

### Authentication Data

For didactic purposes this information is made available to the readers but in a real application *THIS SHOULD NEVER BE MADE PUBLIC*.

- Admin Token: Generated on 23 Sep 2019

```
eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.ezAwNGI0NDU3LTcxYzMtNDQzOS1hMWIyLTAzODIwMjYzYjU5Y30.DexPAjHfp-Pl3R1_dzDOsTAkmZcQ55Ro1AJpj8VhIqo
```

For Admin users:

- `export JWT_SECRET_KEY="-*5h0pp1ng_k4rt*-"`
- `export JWT_CLAIM="{004b4457-71c3-4439-a1b2-03820263b59c}"`

For access token (manipulation of the shopping cart):

- `export ACCESS_TOKEN_SECRET_KEY="5h0pp1ng_k4rt"`

## Endpoints

#### Health check

- `GET /v1/healthcheck` - API status

#### Items

- `GET /v1/items` - lists the existing items.
- `POST /v1/items` - (ADMIN) takes a stream of `org.typelevel.brickstore.dto.BrickToCreate` objects
(see `input-data.jsonl` file for an example of the format), returns an `org.typelevel.brickstore.dto.ImportResult`
for each line (for successes - the brick's ID, for failures - a `cats.data.NonEmptyList` of errors).

#### Cart

- `GET /v1/cart` - gets the current user's cart (the user is hardcoded to id=1, so everyone is that user ;).
The result is a non-empty list of `org.typelevel.brickstore.dto.CartBrick`, or 404 if there isn't anything in the cart.

##### Response Body

```json
[
  {
    "item": {
      "uuid": "6c617c1c-dce0-11e9-8a34-2a2ae2dbcce4",
      "name": "foo",
      "description": "bar",
      "price": 100
    },
    "quantity": 1
  },
  {
    "item": {
      "uuid": "89957bd0-dce0-11e9-8a34-2a2ae2dbcce4",
      "name": "foo",
      "description": "bar",
      "price": 100
    },
    "quantity": 2
  }
]
```

- `POST /v1/cart` - takes a `org.typelevel.brickstore.dto.CartAddRequest` and adds a brick to the cart.
If any errors occur, a NEL of values of type `org.typelevel.brickstore.dto.CartAddError` will be returned.

##### Request Body

```json
{
	"items": {
	  "6c617c1c-dce0-11e9-8a34-2a2ae2dbcce4": 1,
	  "89957bd0-dce0-11e9-8a34-2a2ae2dbcce4": 2
  }
}
```

- `DELETE /v1/cart/{uuid}` - deletes an item from the cart
- `PUT /v1/cart/{uuid}` - modifies quantity from the items, if they are present

##### Request Body

```json
{
	"items": {
	  "89957bd0-dce0-11e9-8a34-2a2ae2dbcce4": 5
  }
}
```


#### Orders

- `POST /v1/order` - creates an order, if the user has a non-empty cart. Fails otherwise.
- `GET /v1/order/stream` - streams existing and incoming order summaries (id, user id, total price). More below.

If you open the `GET /v1/order/stream` endpoint and let it run, you'll see a new item for every order
created after you call it. For best results, you can use the `httpie` CLI tool to convert the streamed lines of JSON
to a pretty format:

## TODO

- Add `payments` management when creating an `order`? Something like https://github.com/kubukoz/talks/blob/master/http4s-doobie-micro/code/payments/src/main/scala/com/app/payments/PaymentsApp.scala

