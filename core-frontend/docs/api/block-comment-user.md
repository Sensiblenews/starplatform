# Comment User Block API

Endpoints used to block comment authors by device ID.

## Overview
- Identity: device ID based (no user accounts).
- Scope: comments only (no global scope).
- Response: `result` is uppercase `OK` or `FAIL`.

## Endpoints
### Block a user
- Method: `POST`
- URL: `/api/super/block/add`

Request body:
```json
{
  "blockerDeviceId": "string",
  "blockedDeviceId": "string",
  "blockedNickname": "string"
}
```

Response examples:
```json
{ "result": "OK" }
```
```json
{ "result": "FAIL", "msg": "Already blocked" }
```

### Unblock a user
- Method: `POST`
- URL: `/api/super/block/remove`

Request body:
```json
{
  "blockerDeviceId": "string",
  "blockedDeviceId": "string"
}
```

Response examples:
```json
{ "result": "OK" }
```
```json
{ "result": "FAIL", "msg": "Not found" }
```

### List blocked users
- Method: `POST`
- URL: `/api/super/block/list`

Request body:
```json
{
  "blockerDeviceId": "string"
}
```

Response example:
```json
{
  "result": "OK",
  "list": [
    {
      "blockedDeviceId": "string",
      "blockedNickname": "string"
    }
  ]
}
```

Response example (failure):
```json
{ "result": "FAIL", "msg": "Invalid deviceId" }
```

## Notes
- `blockedNickname` is display-only and may be non-unique.
- The comment list should be filtered by `blockedDeviceId` on the client or server.
