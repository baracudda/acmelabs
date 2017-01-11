<?php
namespace BitsTheater\actors ;
use BitsTheater\actors\Understudy\BitsApiOnlyActor as BaseActor ;
use BitsTheater\BrokenLeg ;
use BitsTheater\costumes\APIResponse ;
use Exception ;
{ //namespace begin

/**
 * Descend this actor from BitsApiOnlyActor so that all public methods will
 * automatically require Authentication Headers whenever they are called
 * so we can avoid naming every method starting with "api*".
 */
class APIEndpoints extends BaseActor
{

	/**
	* POST server-side example.
	*/
	public function uploadWifiInformation()
	{
		$v =& $this->scene ;
		$this->viewToRender( 'results_as_json' ) ;
		
		if( ! $this->isAllowed( 'api', 'access' ) )
			throw BrokenLeg::toss( $this, 'FORBIDDEN' ) ;
		$wifiFrequency = ( isset( $v->frequency ) ? $v->frequency : null ) ;
		$wifiLinkSpeed = ( isset( $v->link_speed ) ? $v->link_speed : null ) ;
		try {
			$saveResults = $this->saveUploadedWifiInformation( $wifiFrequency, $wifiLinkSpeed ) ;
			$v->results = APIResponse::resultsWithData( $saveResults ) ;
		} catch (Exception $x) {
			throw BrokenLeg::tossException($this, $x);
		}
	}
	
	/**
	 * Save the uploaded data. Typically, a model would be used to save the
	 * data somewhere.
	 * @param string $wifiFrequency
	 * @param string $wifiLinkSpeed
	 * @return Returns the entire record of saved data.
	 */
	protected function saveUploadedWifiInformation( $wifiFrequency, $wifiLinkSpeed )
	{
		// Saves uploaded wifi information to database, throwing Exceptions on errors.
		// Example code for saving data not provided at this time.
		
		return array( 'frequency' => $wifiFrequency, 'link_speed' => $wifiLinkSpeed ) ;
	}
	
} // end class

} // end namespace
