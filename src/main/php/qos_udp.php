<?php

include("config.php");

function qos($server, $port, $timeout, $request)
{
	$response = "1"; // default response is 1 OK

	if(!($sock = socket_create(AF_INET, SOCK_DGRAM, 0)))
	{
		// Can not create socket
		$errorcode = socket_last_error();
		$errormsg = socket_strerror($errorcode);
	}
	else if(!socket_sendto($sock, $request , strlen($request) , 0 , $server , $port))
	{
		// Can not send packet
		$errorcode = socket_last_error();
		$errormsg = socket_strerror($errorcode);
	}
	else
	{
		socket_set_option($sock, SOL_SOCKET, SO_RCVTIMEO, $timeout);
		if(socket_recv ($sock, $response, 2045, MSG_WAITALL ) === FALSE)
		{
			// Can not receive packet
			$errorcode = socket_last_error();
			$errormsg = socket_strerror($errorcode);
		}
	}

	return $response;
}

$key = $_GET['key'];
$hash = crc32($key);
$partition = $hash % count($servers);
$server = $servers[$partition];
$qos = qos($server['host'], $server['port'], $timeout, $key);
echo "$qos";
//echo "Key: $key, QoS: $qos, Hash: $hash, Partition: $partition";
?>
