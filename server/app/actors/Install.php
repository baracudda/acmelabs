<?php
namespace BitsTheater\actors ;
use BitsTheater\actors\Understudy\BitsInstall as BaseActor ;
use com\blackmoonit\database\DbConnInfo ;
{

/**
 * Provides custom installation actions for the ACME web service.
 */
class Install extends BaseActor
{
	/**
	 * The custom table space for ACME data tables. This will be separate from
	 * the `webapp` tables that come with vanilla BitsTheater.
	 * @var string
	 */
	const ACME_DB_CONN_NAME = 'acme' ;

	/**
	 * (Override) Adds the ACME database connection scheme to the vanilla
	 * `webapp` scheme.
	 * @see \BitsTheater\actors\Understudy\BitsInstall::getDbConns()
	 */
	public function getDbConns()
	{
		$db_conns = parent::getDbConns() ;

		$theDbConnInfo = DbConnInfo::asSchemeINI( self::ACME_DB_CONN_NAME ) ;
		$theDbConnInfo->dbConnSettings->dbName = '' ;
		$theDbConnInfo->dbConnSettings->host = '' ;
		$theDbConnInfo->dbConnSettings->username = '' ;
		$db_conns[] = $theDbConnInfo ;

		return $db_conns ;
	}
}

} // namespace