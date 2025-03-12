@echo off

REM Read the .env file and set variables
for /f "tokens=1,* delims==" %%a in ('type .env') do (
    if "%%a"=="USERNAME" set "USERNAME=%%b"
    if "%%a"=="PASSWORD" set "PASSWORD=%%b"
)

REM Define the base URL
set "base_url=https://localhost:8888/api/v1/"

REM Define the route
set "route=role"

REM Check if groupVisibility is not defined
if not defined groupVisibility (
    set "groupVisibility=null"
)

REM Define the role-specific data (groupVisibility and roleName)
set "roleName=MedicalAuthority"
set "data={\"groupVisibility\":%groupVisibility%,\"roleName\":\"%roleName%\"}"

REM Make the POST request using curl with basic authentication
curl -s -o response.txt -X POST -d "%data%" -u "%USERNAME%:%PASSWORD%" -k "%base_url%%route%"

REM Check if the request was successful
if %errorlevel% equ 0 (
    echo Request successful for MedicalAuthority!
) else (
    echo An error occurred for MedicalAuthority: %errorlevel%
)

REM Define the role-specific data (groupVisibility and roleName) for Patient
set "roleName=Patient"
set "data={\"groupVisibility\":%groupVisibility%,\"roleName\":\"%roleName%\"}"

REM Make the POST request using curl with basic authentication
curl -s -o response.txt -X POST -d "%data%" -u "%USERNAME%:%PASSWORD%" -k "%base_url%%route%"

REM Check if the request was successful
if %errorlevel% equ 0 (
    echo Request successful for Patient!
) else (
    echo An error occurred for Patient: %errorlevel%
)

REM Define the role-specific data (groupVisibility, permissionString, and permissionType) for permission
set "permissionString=myPermission"
set "permissionType=read"
set "data_permission={\"groupVisibility\":%groupVisibility%,\"permissionString\":\"%permissionString%\",\"permissionType\":\"%permissionType%\"}"

REM Make the POST request for permission using curl with basic authentication
curl -s -o response_permission.txt -X POST -d "%data_permission%" -u "%USERNAME%:%PASSWORD%" -k "%base_url%%route_permission%"

REM Check if the request for permission was successful
if %errorlevel% equ 0 (
    echo Request successful for permission!
) else (
    echo An error occurred for permission: %errorlevel%
)