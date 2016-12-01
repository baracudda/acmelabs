<?php
namespace BitsTheater\res ;
use BitsTheater\res\BitsWebsite as BaseResources ;
use Exception ;
{ //begin namespace

/**
 * Defines the parameters of this phpBitsTheater-based web service.
 */
class Website extends BaseResources
{
	public $feature_id = 'acme: website' ;      // DO NOT TRANSLATE this label.
	public $version_seq = 1 ;
	public $version = '1.0' ;		// displayed version text
	public $api_version_seq = 1 ; 	// API version; inc when actors change

	public $list_patrons_html = array(
			'prime_investor' => '<a href="http://acmelabs.example.com/">ACME Labs</a>',
		);

	//public $list_credits_html_more = array(
	//);

	/**
	 * Some resources need to be initialized by running code rather than a
	 * static definition.
	 * @see \BitsTheater\res\Website::setup()
	 */
	public function setup( $aDirector )
	{
		parent::setup($aDirector) ;

		$this->res_array_merge( $this->js_libs_load_list, array(
				'jquery-ui/jquery-ui.min.js',
			));

		$this->res_array_merge( $this->js_load_list, array(
				'webapp_mini.js' => WEBAPP_JS_URL,
				'BitsRightGroups.js' => WEBAPP_JS_URL,
			));

		//$this->res_array_merge($this->list_credits_html, $this->list_credits_html_more);
	}

	/**
	 * Override this function if your website needs to do some updates that are
	 * not database related.
	 * Throw an exception if your update did not succeed.
	 * @param number $aSeqNum - the version sequence number (<= what is defined
	 *  in your overridden Website class).
	 * @throws Exception on failure.
	 */
	public function updateVersion( $aSeqNum )
	{
		try
		{
			//nothing to do, yet
		}
		catch( Exception $x )
		{
			$this->debugLog( __METHOD__ . ' failed: ' . $x->getMessage() ) ;
			throw $x ;
		}
	}

} //end class

} //end namespace
