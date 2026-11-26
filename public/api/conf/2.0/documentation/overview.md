This API allows a third party to retrieve real time income information for an individual by specifying a required fieldset.

This version of the API is in development and is very likely to change.

The current API allows the a consuming service to retrieve an individuals income information by providing a list of required fields.

To use this API, several matching fields are required as well as a list of filter field keys. This will determine which values are returned as part of the response. The list of keys which can be requested can be found in the request schema below.

If a key is requested that is not in the list of potential keys, a 400 INVALID_PAYLOAD response will be returned.
