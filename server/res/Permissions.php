<?php
namespace BitsTheater\res ;
use BitsTheater\Director;
use BitsTheater\res\BitsPermissions as BasePermissions ;
{
	/**
	 * Defines permission groups for the ACME Labs service.
	 *
	 * NOTE: This file directly overlays the same file provided by phpBitsTheater.
	 * If you push up a new BitsTheater version to the server, then this file
	 * MUST also be pushed up afterward; otherwise permission settings will be
	 * improperly resolved at runtime.
	 *
	 * @see BitsTheater\res\en\Permissions
	 */
	class Permissions
	extends BasePermissions
	{
		public $enum_my_namespaces = array(
				'api'
		);

		public $enum_api = array(
				'access'
		);

		public function setup( Director $aDirector )
		{
			$this->res_array_merge( $this->enum_namespace, $this->enum_my_namespaces ) ;
			parent::setup($aDirector) ;
		}
	}

}