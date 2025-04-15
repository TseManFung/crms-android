package com.fyp.crms_backend.utils

import com.crms.crmsAndroid.R

/**
 * 页面权限枚举，每个页面对应一个二进制位 (0-31)
 * 权限值通过 [flag] 属性表示 (2^position)
 */
enum class AccessPagePermission(val flag: Int) {
    // 0-3 位
    INVENTORY_FRAGMENT(1 shl 0),            // 2^0 = 1
    MANUAL_INVENTORY_FRAGMENT(1 shl 1),     // 2^1 = 2
    SEARCH_ITEM_FRAGMENT(1 shl 2),          // 2^2 = 4
    UPDATE_ROOM_ITEM_FRAGMENT(1 shl 3),     // 2^3 = 8

    // 4-7 位
    UPDATE_ITEM_LOCATION_FRAGMENT(1 shl 4), // 2^4 = 16
    ADD_ITEM_FRAGMENT(1 shl 5),             // 2^5 = 32
    DELETE_ITEM_FRAGMENT(1 shl 6),          // 2^6 = 64
    NEW_ROOM_FRAGMENT(1 shl 7),             // 2^7 = 128

    // 8-15 位
    HOME_PAGE(1 shl 8),                     // 2^8 = 256
    BORROW_PAGE(1 shl 9),                   // 2^9 = 512
    RETURN_PAGE(1 shl 10),                  // 2^10 = 1024
    ADD_USER_PAGE(1 shl 11),                // 2^11 = 2048
    GENERATE_REPORT_PAGE(1 shl 12),         // 2^12 = 4096
    MANAGE_CAMPUS_PAGE(1 shl 13),           // 2^13 = 8192
    MANAGE_ROOM_PAGE(1 shl 14),            // 2^14 = 16384
    MANAGE_ITEM_PAGE(1 shl 15);             // 2^15 = 32768

    companion object {
        // 最大支持的页面数量
        const val MAX_PAGES = 32

        /**
         * 将权限集合转换为整数
         */
        fun toInt(permissions: Set<AccessPagePermission>): Int {
            return permissions.fold(0) { acc, permission ->
                acc or permission.flag
            }
        }

        /**
         * 从整数解析出权限集合
         */
        fun fromInt(value: Int): Set<AccessPagePermission> {
            return enumValues<AccessPagePermission>()
                .filter { (it.flag and value) != 0 }
                .toSet()
        }

        /**
         * 检查某个权限是否被授予
         */
        fun hasPermission(value: Int, permission: AccessPagePermission): Boolean {
            return (value and permission.flag) != 0
        }

        fun defaultAccessPage(accessLevel: Int): Int {
            return when (accessLevel) {
                0 -> 65535
                100 -> 63487
                1000 -> 1540
                else -> 0
            }
        }

        val fragmentPermissions = mapOf(
            R.id.nav_inventory to INVENTORY_FRAGMENT,
            R.id.nav_manInventory to MANUAL_INVENTORY_FRAGMENT,
            R.id.searchItem to SEARCH_ITEM_FRAGMENT,
            R.id.nav_updateItem to UPDATE_ROOM_ITEM_FRAGMENT,
            R.id.nav_updateLoc to UPDATE_ITEM_LOCATION_FRAGMENT,
            R.id.nav_addItem to ADD_ITEM_FRAGMENT,
            R.id.nav_deleteItem to DELETE_ITEM_FRAGMENT,
            R.id.nav_newRoom to NEW_ROOM_FRAGMENT,
        )
    }
}