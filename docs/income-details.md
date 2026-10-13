# Income details
Returns the earnings information (income details) which has been matched to a citizen by Real Time Information. The information returned will be identified by a National Insurance number and income details within a specified period (date range).

## API
POST /individuals/:correlationId/income

### Request
#### Headers
| Name | Value |
| --- | --- |
|Authorization|Bearer [token here]|

#### Path Parameters
| Name | Type | Value | Example |
| --- | --- | --- | --- |
|correlationId|String|A valid correlation ID.|`beb649f5-7da4-4d3d-8b8c-936574286b1d`|

### Response


#### Successful Response
200

#### Error Responses

If the downstream service is unavailable then a 503 is returned.

If the correlation ID or the date range of payload is invalid then a 400 is returned.

If a list of downstream failures is returned then 400 is returned

If there is an issue with the request then a 400 is returned.

If the liability is not found a 404 is returned with the following Json body:-
```json
{
  "error": "Liability not found"
}
```
