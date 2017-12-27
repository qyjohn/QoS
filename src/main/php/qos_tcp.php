<?php

include("config.php");

function qos($server, $port, $timeout, $request)
{
	$response = "1"; // default response is 1 OK

	if(!($socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP)))
	{
		// Can not create socket
		$errorcode = socket_last_error();
		$errormsg = socket_strerror($errorcode);
	}
	else if (!socket_connect($socket, $server, $port))
	{
		// Can not connect to server
		$errorcode = socket_last_error();
		$errormsg = socket_strerror($errorcode);
	}
	else if(!socket_write($socket, $request , strlen($request)))
	{
		// Can not send packet
		$errorcode = socket_last_error();
		$errormsg = socket_strerror($errorcode);
	}
	else
	{
		socket_set_option($socket, SOL_SOCKET, SO_RCVTIMEO, $timeout);
		$response = socket_read($socket, 32);
		socket_close($socket);
	}

	return $response;
}

$key = $_GET['key']."\n";
$hash = crc32($key);
$partition = $hash % count($servers);
$server = $servers[$partition];
//$qos = qos($server['host'], $server['port'], $timeout, $key);
//echo "$qos";
echo "1";
?>
