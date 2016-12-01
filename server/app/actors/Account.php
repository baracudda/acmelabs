<?php
namespace BitsTheater\actors ;
use BitsTheater\actors\Understudy\AuthBasicAccount as BaseActor ;
{

class Account extends BaseActor
{
	/**
	 * A magic token thrown at us by clients who want authentication.
	 * @var string
	 */
	const MAGIC_PING_TOKEN = 'AcMe' ;
	
} // class

} // namespace