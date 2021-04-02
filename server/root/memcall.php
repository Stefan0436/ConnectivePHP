<html>
	<head>
	<title>Hello World through PHP /w Memcall</title>
	</head>
	<body>
	<h1>Non-memcall PHP header, the next is memcall</h1>
	</body>
</html>
<?php
header("X-Connective-Memcall: php.test.memcalltest.one");
header("X-Connective-Memcall-2: php.test.memcalltest.two; test");
header("X-Connective-Memcall-3: php.test.memcalltest.three; one two three");
?>