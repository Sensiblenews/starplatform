<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<html>

<head>
    <title>Home</title>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
    <meta name="viewport" content="width=device-width,initial-scale=1">
</head>
<body>
<xmp>${appleInfo}</xmp>
<script>
    $(document).ready(function() {
        var appleInfo = '${appleInfo}';
        if(appleInfo != ""){
            var data = JSON.parse(appleInfo);
            if(data['sub'] != null) {
                console.info(data);
            }
        }
    });
</script>
</body>
</html>