# real-time-income-information

This API will return the earnings information (income details) which has been matched to a citizen by Real Time Information. The information returned will be identified by a National Insurance number and income details within a specified period (date range).

This is a private API microservice - you will need to register your application to get access to this API.
https://developer.service.hmrc.gov.uk/api-documentation/docs/using-the-hub

This API will return the earnings information (income details) which has been matched to a citizen by Real Time Information. The information returned will be identified by a National Insurance number and income details within a specified period (date range).

## Running the Application

```bash
sm2 --start RTII_ALL
```

## Running the Application Locally

Follow the instructions above for running the application, then:

```bash
sm2 --stop REAL_TIME_INCOME_INFORMATION
sbt run
```

## Tests

Below command will run unit tests, integration tests, formatting and coverage:

```bash
./run_all_tests.sh
```

## Licence

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).