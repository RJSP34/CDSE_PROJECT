$REST_API_HOST = "localhost"
$REST_API_PORT = 8888
$P2P_GATEWAY_HOST = "localhost"
$P2P_GATEWAY_PORT = 8080

$REST_API_URL="https://${REST_API_HOST}:${REST_API_PORT}/api/v5_1"
$REST_API_USER = "admin"
$REST_API_PASSWORD = "admin"
$AUTH_INFO = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes(("${REST_API_USER}:${REST_API_PASSWORD}" -f $username,$password)))

$WORK_DIR = "creating-mgm-cpi"
md $WORK_DIR

Add-Content $WORK_DIR/GroupPolicy.json @"
{
  "fileFormatVersion" : 1,
  "groupId" : "1",
  "registrationProtocol" :"net.corda.membership.impl.registration.dynamic.mgm.MGMRegistrationService",
  "synchronisationProtocol": "net.corda.membership.impl.synchronisation.MgmSynchronisationServiceImpl"
}
"@