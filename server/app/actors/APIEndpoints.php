<?php
namespace BitsTheater\actors ;
use BitsTheater\actors\Understudy\BitsApiOnlyActor as BaseActor ;
use BitsTheater\BrokenLeg ;
use BitsTheater\costumes\APIResponse ;
{ //namespace begin

class APIEndpoints extends BaseActor
{
    public function ajajPing()
    {
        $this->viewToRender( 'results_as_json' ) ;
        $this->scene->results = new APIResponse() ;
    }

    /**
    * POST server-side example.
    */
    public function uploadWifiInformation()
    {
        if( ! $this->isAllowed( 'api', 'access' ) )
            throw BrokenLeg::toss( $this, 'FORBIDDEN' ) ;
        $v =& $this->scene ;
        $wifiFrequency = ( isset( $v->frequency ) ? $v->frequency : null ) ;
        $wifiLinkSpeed = ( isset( $v->link_speed ) ? $v->link_speed : null ) ;
        $saveResults = $this->saveUploadedWifiInformation( $wifiFrequency, $wifiLinkSpeed ) ;
        $v->results = APIResponse::resultsWithData( $saveResults ) ;
    }
    
    protected function saveUploadedWifiInformation( $wifiFrequency, $wifiLinkSpeed )
    {
        // Saves uploaded wifi information to database, throwing a BrokenLeg on errors.
        return array( 'frequency' => $wifiFrequency, 'link_speed' => $wifiLinkSpeed ) ;
    }
} // end class

} // end namespace