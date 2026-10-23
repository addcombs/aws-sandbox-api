# AWS Sandbox API

## Cognito configuration

The application defaults to the current `aws-sandbox-1` pool and browser app client. For another
environment, override these values before starting the API:

```text
COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/<user-pool-id>
COGNITO_APP_CLIENT_ID=<public-spa-app-client-id>
COGNITO_USER_INFO_URI=https://<user-pool-domain>.auth.us-east-1.amazoncognito.com/oauth2/userInfo
```

The Angular app uses the matching public pool and client identifiers in
`aws-sandbox-ui/src/app/auth/cognito-auth.config.ts`. Configure the Cognito app client for the
authorization-code grant with PKCE, the `openid`, `email`, and `profile` scopes, and the exact
frontend callback and sign-out URLs. Do not create a client secret for the browser app.

`GET /api/health` remains public. All other API routes require a valid Cognito access token. On
`GET /api/me`, the API validates the token's signature, issuer, expiration, token use, and app-client
ID, then retrieves the verified profile from Cognito's UserInfo endpoint and upserts it by Cognito
`sub`. The API does not accept client-supplied identity fields. The former `/api/users` directory
and CRUD routes are intentionally unavailable so a signed-in user cannot access other users'
profiles.
