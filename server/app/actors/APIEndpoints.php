<?php
namespace BitsTheater\actors ;
use BitsTheater\actors\Understudy\BitsInstall as BaseActor ;
use BitsTheater\BrokenLeg ;
use BitsTheater\costumes\APIResponse ;
{ //namespace begin

class APIEndpoints extends BaseActor
{
	const SUBSTITUTE_NAME_TOKEN = 'NAME:' ;

    public function ajajPing()
    {
        $this->viewToRender( 'results_as_json' ) ;
        $this->scene->results = new APIResponse() ;
    }

    /**
    * POST server-side example.
    */
    public function uploadDeviceInformation()
    {
        if( ! $this->isAllowed( 'api', 'access' ) )
            throw BrokenLeg::toss( $this, 'FORBIDDEN' ) ;
        $v =& $this->scene ;
        $theDeviceName = ( isset( $v->device_name ) ? $v->device_name : null ) ;
        $theDeviceID = ( isset( $v->device_id ) ? $v->device_id : null ) ;
        if( $theDeviceID == null && $theDeviceName != null )
            $theDeviceID = self::SUBSTITUTE_NAME_TOKEN . $theDeviceName ;
        if( $theDeviceID == null )
            throw BrokenLg::toss( $this, 'MISSING_ARGUMENT', $theDeviceID ) ;
        $saveResults = $this->saveUploadedDeviceInformation( $theDeviceID ) ;
        $v->results = APIResponse::resultsWithData( $saveResults ) ;
    }
    
    protected function saveUploadedDeviceInformation( $aDeviceID )
    {
        // Saves uploaded device information to database, throwing a BrokenLeg on errors.
        return $aDeviceID ;
    }
} // end class

} // end namespace