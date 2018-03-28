## Running integration tests

To run the integration tests, [clone this test base](https://airtable.com/shrCyRokgg5JsFJ8B).

Once you've done that, you'll need to set three environment variables:

* `AIRTABLE_API_KEY` is your Airtable API key (grab it from [airtable.com/account](https://airtable.com/account))
* `AIRTABLE_BASE` is the base ID (looks like `appAbCdeFgHiJkLmN`)
* `AIRTABLE_TABLE` is the name of the Airtable table (will be `My Table`)

You can do this however you like, but something like this should work:

```sh
AIRTABLE_API_KEY=keyAbCdEfGhIjKlMn AIRTABLE_BASE=appAbCdeFgHiJkLmN AIRTABLE_TABLE='My Table' lein test
```
