$REST_API_HOST = ""
$REST_API_PORT = 
$P2P_GATEWAY_HOST = ""
$P2P_GATEWAY_PORT = 

$REST_API_URL="https://${REST_API_HOST}:${REST_API_PORT}/api/v5_1"
$REST_API_USER = ""
$REST_API_PASSWORD = ""
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
