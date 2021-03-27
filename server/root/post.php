<?php 
$str = file_get_contents('php://input');
echo($str);
echo("\n");
echo($_POST["test"]);
echo("\n");
?>