package dev.lisek.openyi

/**
 * This file contains message IDs used for communication with the Yi camera.
 * The message IDs were collected from various online sources and open-source libraries.
 * Language models were used to find sparse information on some of the less understood ones.
 * They were tested and documented against the Yi 1080p Action Camera (v23), firmware 1.2.13.
 *
 * The list is still incomplete, some IDs report rval != -23 (which means they are implemented),
 * but I have not found the correct parameters for them yet.
 * Those IDs are commented out with suspected names and usage for future reference.
 *
 * RVAL values:
 * - 0: Success
 * - -1: Unknown error
 * - -4: Invalid token
 * - -7: Malformed JSON
 * - -9: Missing header (type/param)
 * - -13: Invalid option
 * - -14: Not permitted (e.g. not recording for MSG_STOP_RECORD)
 * - -15: Invalid setting name
 * - -23: Not implemented
 * - -24: Invalid type
 * - -25: Invalid param
 * - -26: Invalid path
 *
 * Other RVAL values are documented in various online sources,
 * but I have personally not seen them in my testing (yet).
 *
 * Payload info omits msg_id and rval as they are (nearly) always present in the response.
 */


/**
 * Get the value of a specific setting.
 *
 * Required payload:
 * - `"param": "<setting_name>"`
 *
 * Response payload:
 * - `"param": "<setting_value>"`
 */
const val MSG_GET_SETTING = 1
/**
 * Set the value of a specific setting.
 *
 * Required payload:
 * - `"type": "<setting_name>"`
 * - `"param": "<setting_value>"`
 *
 * Response payload:
 * - `"type": "<setting_name>"`
 * - `"param": "<setting_value>"`
 */
const val MSG_SET_SETTING = 2
/**
 * Get all settings and their values.
 *
 * Required payload: none
 *
 * Response payload:
 * - `"param": <{"<setting_name>": "<setting_value>"}, ...>`
 */
const val MSG_GET_ALL_SETTINGS = 3
/** TODO: check
 * Format the SD card.
 *
 * Required payload: none
 *
 * Response payload: none
 */
const val MSG_FORMAT_SD = 4
/**
 * Get the storage status of the SD card.
 *
 * Required payload:
 * - `"type": "<storage_type>"` ("total", "free")
 *
 * Response payload:
 * - `"param": <size_bytes>}`
 */
const val MSG_GET_SD_STATUS = 5
/**
 * Get the number of files on the SD card.
 *
 * Required payload:
 * - `"type": "<storage_type>"` ("photo", "video", "total")
 *
 * Response payload:
 * - `"param": <file_count>`
 */
const val MSG_GET_SD_FILE_COUNT = 6
/**
 * Notification messages for session events (responses of msg_id 257 handshake).
 *
 * Not callable (returns rval -23 if called).
 *
 * Received payload:
 * - `"type": "<notification_type>"`
 * - (optional) `"param": <notification_param>`
 */
const val MSG_NOTIFICATION = 7
/**
 * TODO: Check. I didn't prepare any firmware.bin to flash, as to not
 *       brick my camera before thoroughly testing everything else.
 *
 * Flash a firmware update to the camera.
 *
 * Required payload:
 * - (optional) `"param": "<firmware_file_path>"`; defaults to $PWD/"firmware.bin"
 *
 * Response payload: none
 */
const val MSG_BURN_FW = 8
/**
 * Get the available choices for a specific setting.
 *
 * Required payload:
 * - `"param": "<setting_name>"`
 *
 * Response payload:
 * - `"param": "<setting_name>"`
 * - `"permission": "<settable>"`
 * - `"options": < "<choice1>", "<choice2>", ... >`
 */
const val MSG_GET_SETTING_CHOICES = 9
/**
 * Not 100% sure about this one, but it seems to be a stub message that does nothing.
 *
 * Required payload: none
 *
 * Response payload: none
 */
const val MSG_STUB = 10
/**
 * Get device information (model, firmware version, etc.). Mostly useless.
 *
 * Useful information (e.g. revision, firmware version) is in settings (msg_id 3).
 */
const val MSG_GET_DEV_INFO = 11
// msg_id 12 is not implemented (rval -23)
/**
 * Get the battery level and charging status.
 *
 * Required payload: none
 *
 * Response payload:
 * - `"type": <power_source>` ("battery", "adapter")
 * - `"param": "<battery_level>"`
 */
const val MSG_GET_BATTERY = 13
/**
 * Set the zoom level.
 *
 * Required payload:
 * - `"type": "<zoom_type>"` ("current")
 * - `"param": "<zoom_level>"` (0-100)
 *
 * Response payload:
 * - `"param": "<zoom_level>"`
 */
const val MSG_SET_ZOOM = 14
/**
 * Get the zoom level.
 *
 * Required payload:
 * - `"type": "<zoom_type>"` ("current")
 *
 * Response payload:
 * - `"param": "<zoom_level>"`
 */
const val MSG_GET_ZOOM = 15
/** TODO: check
 * Set the camera's bitrate.
 *
 * Required payload:
 * - `"param": "<bitrate>"`
 *
 * Response payload:
 * - `"param": "<bitrate>"`
 */
const val MSG_SET_BITRATE = 16

/**
 * Start the session. Handshake, obtain token, receive notifications.
 *
 * Required payload: none
 *
 * Response payload:
 * - `"param": <session_token>`
 */
const val MSG_START_SESSION = 257
/**
 * Stop the session. Disconnect, invalidate token, stop notifications.
 *
 * Required payload: none
 *
 * Response payload: none
 */
const val MSG_STOP_SESSION = 258
/**
 * (Re)start the camera preview.
 *
 * Doesn't seem to work for v23 (rval -1).
 *
 * Required payload: none?
 *
 * Response payload: none?
 */
const val MSG_START_PREVIEW = 259
/**
 * Stop the camera preview.
 *
 * Required payload: none
 *
 * Response payload: none
 */
const val MSG_STOP_PREVIEW = 260

const val MSG_START_RECORD = 513
const val MSG_STOP_RECORD = 514
const val MSG_GET_RECORD_STATUS = 515

const val MSG_TAKE_PHOTO = 769
const val MSG_STOP_PHOTO = 770

const val MSG_GET_THUMBNAIL = 1025
/**
 * Get information about a specific file.
 *
 * Required payload:
 * - `"param": "<file_path>"`
 *
 * Response payload:
 * - `"param": {"size": <size>, "date": "<date>", "resolution": "<resolution>", "media_type": "<media_type>"}`
 */
const val MSG_GET_FILE_INFO = 1026

/** TODO: check
 * Delete a file.
 *
 * Required payload:
 * - `"param": "<file_path>"`
 *
 * Response payload: none
 */
const val MSG_DELETE_FILE = 1281
/**
 * List files in a directory.
 *
 * Required payload:
 * - (optional) `"param": "<path>"`; defaults to pwd (check: msg_id 1284)
 *
 * Response payload:
 * - `"listing": <{"<name>": "<date>"}, ...>`
 */
const val MSG_LIST_FILES = 1282
/**
 * Change the camera's working directory.
 *
 * Required payload:
 * - `"param": "<new_pwd>"`
 *
 * Response payload:
 * - `"param": "<new_pwd>"`
 */
const val MSG_CHANGE_PWD = 1283
/**
 * Get the camera's working directory.
 *
 * Required payload: none
 *
 * Response payload:
 * - `"param": "<pwd>"`
 */
const val MSG_GET_PWD = 1284
const val MSG_GET_FILE = 1285
const val MSG_PUT_FILE = 1286
const val MSG_CANCEL_TRANSFER = 1287

/**
 * Restart the camera's Wi-Fi module into AP mode.
 *
 * Default wifi.conf is used, overriding any custom config.
 *
 * Required payload: none
 *
 * Response payload: none
 */
const val MSG_WIFI_RESTART = 1537

/**
 * Camera terminates the connection.
 *
 * Sent e.g. when a new connection is established.
 *
 * Sending this msg_id to the camera does not yield any response.
 *
 * Received payload: {"msg_id":1793} (no rval)
 */
const val MSG_TERMINATE_CONNECTION = 1793

/**
 * Get the SD card type (e.g. SDHC, SDXC).
 *
 * Required payload: none
 *
 * Response payload:
 * - `"param": "<sd_type>"`
 */
const val MSG_GET_SD_TYPE = 16777217

/**
 * Get sample AP info (SSID, password, etc.).
 *
 * This does not seem to pull from an actual configuration file,
 * as the AP_SSID is not matching to what the camera actually exposes.
 * I couldn't find any .conf file that would contain this information, either.
 *
 * The output JSON is malformed, as the content is not wrapped in any
 * JSON primitive (string, array, object, etc).
 *
 * Probably useless for anything. Stored here for reference.
 *
 * Required payload: none
 *
 * Response payload:
 * - Raw variable config for AP mode (no quotes)
 */
const val MSG_GET_AP_INFO = 33554434
