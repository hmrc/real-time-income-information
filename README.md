# Real-time income information

## About

This API is part of the PTA service and returns the earnings information (income details) which has been matched to a citizen by Real Time Information. The information returned will be identified by a National Insurance number and income details within a specified period (date range).

ENDPOINTS
---

| *Endpoint* | *Supported Methods* | *Description* |
|--------|----------------------|---------------|
|[/individuals/:correlationId/income](./docs/income-details.md) | POST | Returns the earnings information (income details) which has been matched to a citizen by Real Time Information.|


## Running the service

```bash
sm2 --start RTII_ALL
```

## Testing the service

Below command will run unit tests, integration tests, formatting and coverage:

```bash
./run_all_tests.sh
```

## Licence

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).