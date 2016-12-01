<?php

namespace BitsTheater\models;
use BitsTheater\models\PropCloset\AuthBasic as BaseModel;
use BitsTheater\costumes\HttpAuthHeader;
use BitsTheater\costumes\AccountInfoCache;
use BitsTheater\costumes\SqlBuilder;
use PDOException;
use BitsTheater\models\Accounts; /* @var $dbAccounts Accounts */
use BitsTheater\models\AuthGroups; /* @var $dbAuthGroups AuthGroups */
use com\blackmoonit\Strings;
{//namespace begin

class Auth extends BaseModel
{
	/**
	 * The name of the model which can be used in IDirected::getProp().
	 * @var string
	 * @since BitsTheater 3.6.1
	 */
	const MODEL_NAME = __CLASS__ ;

	/**
	 * Descendants may wish to further scrutinize header information before allowing access.
	 * @param HttpAuthHeader $aAuthHeader - the header info.
	 * @param array $aMobileRow - the mobile row data.
	 * @param AccountInfoCache $aUserAccount - the user account data.
	 * @return boolean Returns TRUE if access is allowed.
	 */
	protected function checkHeadersForMobileCircumstances(HttpAuthHeader $aAuthHeader, $aMobileRow, AccountInfoCache $aUserAccount) {
		if (parent::checkHeadersForMobileCircumstances($aAuthHeader, $aMobileRow, $aUserAccount)) {
			//check circumstances like is GPS outside pre-determined bounds
		}
		return true;
	}
	
	/**
	 * API fingerprints from mobile device. Recomended that
	 * your website mixes their order up, at the very least.
	 * @param string[] $aFingerprints - string array of device info.
	 * @return string[] Return a keyed array of device info.
	 */
	public function parseAuthBroadwayFingerprints($aFingerprints) {
		if (!empty($aFingerprints)) {
			$theIMEI = Strings::endsWith($aFingerprints[2],'1')
				? substr($aFingerprints[2], 0, -1)
				: null
			;
			//function being used to return the IMEI appends a '1' if successful, else
			//  alternate numbers are returned with different numbers appended depending on
			//  what is returned eventually. e.g. tablets that do not have IMEI may return
			//  a serial number instead. ('3' in case you are interested in that one)
			return array(
					'app_signature' => $aFingerprints[0],
					'mobile_id' => $aFingerprints[1],
					'device_id' => $theIMEI,
					'device_locale' => $aFingerprints[3],
					'device_memory' => (is_numeric($aFingerprints[4]) ? $aFingerprints[4] : null),
			);
		} else return array();
	}
	
	/**
	 * API circumstances from mobile device. Recommended that
	 * your website mixes their order up, at the very least.
	 * @param string[] $aCircumstances - string array of device meta,
	 * such as current GPS, user device name setting, current timestamp, etc.
	 * @return string[] Return a keyed array of device meta.
	 */
	public function parseAuthBroadwayCircumstances($aCircumstances) {
		if (!empty($aCircumstances)) {
			//NOTE: lat/long may be "?" to indicate GPS is disabled.
			return array(
					'circumstance_ts' => $aCircumstances[0],
					'device_name' => $aCircumstances[1],
					'device_latitude' => (is_float($aCircumstances[2]) ? $aCircumstances[2] : null),
					'device_longitude' => (is_float($aCircumstances[3]) ? $aCircumstances[3] : null),
					'device_gps_disabled' => ($aCircumstances[2]==='?' && $aCircumstances[3]==='?'),
			);
		} else return array();
	}
	
}//end class

}//end namespace
