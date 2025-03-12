package com.r3.developers.csdetemplate.utxoexample.workflows.services

import com.r3.developers.csdetemplate.utxoexample.enums.Permission
import com.r3.developers.csdetemplate.utxoexample.enums.Role

class proprietiesValues {
    companion object {
        private val rolesList: MutableList<Map<Role, Map<Permission, Boolean>>> = mutableListOf()

        init {
            addRole(Role.MedicalAuthority, mapOf(Permission.ALLOW_CREATE_DOCUMENT to true, Permission.ALLOW_SIGNATURE to true, Permission.ALLOW_READ_DOCUMENT to true, Permission.ALLOW_UPDATE_DOCUMENT to true, Permission.ALLOW_DELETE_DOCUMENT to true))
            addRole(Role.Patient, mapOf(Permission.ALLOW_CREATE_DOCUMENT to false, Permission.ALLOW_SIGNATURE to true, Permission.ALLOW_READ_DOCUMENT to true, Permission.ALLOW_UPDATE_DOCUMENT to false, Permission.ALLOW_DELETE_DOCUMENT to false))
            addRole(Role.None, mapOf(Permission.ALLOW_CREATE_DOCUMENT to false, Permission.ALLOW_SIGNATURE to false, Permission.ALLOW_READ_DOCUMENT to true, Permission.ALLOW_UPDATE_DOCUMENT to false, Permission.ALLOW_DELETE_DOCUMENT to false))
        }

        private fun addRole(roleName: Role, roleProperties: Map<Permission, Boolean>) {
            rolesList.add(mapOf(roleName to roleProperties))
        }

        fun getRoles(): List<Map<Role, Map<Permission, Boolean>>> {
            return rolesList
        }

        fun isPermissionAllowed(roleName: Role, permission: Permission): Boolean {
            val role = rolesList.find { it.containsKey(roleName) }
            return role?.get(roleName)?.get(permission) ?: false
        }
    }
}