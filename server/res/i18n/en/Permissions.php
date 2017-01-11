<?php
namespace BitsTheater\res\en ;
use BitsTheater\Director ;
use BitsTheater\res\en\BitsPermissions as BasePermissions ;
{

/**
 * Defines on-screen labels for permissions and permission groups in the ACME
 * Labs web UI.
 * 
 * NOTE: This file directly overlays the same file provided by phpBitsTheater.
 * If you push up a new BitsTheater version to the server, then this file
 * MUST also be pushed up afterward; otherwise permission settings will be
 * improperly resolved at runtime.
 * 
 * @see BitsTheater\res\Permissions
 */
class Permissions
extends BasePermissions
{
	public $label_my_namespaces = array(
			'api' => 'ACME Labs Device API Access'
		);
	
	public $desc_my_namespaces = array(
			'api' => 'ACME Labs Device API Access'
		);
	
	public $label_api = array(
			'access' => 'Allow Access'
		);
	
	public $desc_api = array(
			'access' => 'Grant access to the ACME Labs data service API.'
		);
	
	public function setup( Director $aDirector )
	{
		$this->res_array_merge( $this->label_namespace, $this->label_my_namespaces ) ;
		$this->res_array_merge( $this->desc_namespace, $this->desc_my_namespaces ) ;
		parent::setup( $aDirector ) ;
	}
}

}