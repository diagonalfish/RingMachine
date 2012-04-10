<!-- BEGIN: main -->
<!DOCTYPE html>
<html>
	<head>
		<title>Ring Machine Master Node - Upload a File</title>
		<style type="text/css">
			body {
				font-family: "Georgia", serif;
			}
		</style>
	</head>
	<body>
		<h2>Upload a File</h2>
		
		<a href="?page=index"><< Back to Dashboard</a>
		<br/>
		<br/>
		
		<form method="post" action="?page=uploadprocess" enctype="multipart/form-data">
			Select file: <input type="file" name="uploadfile"></input><br/>
			<button type="submit">Upload</button>
		</form>
		
	</body>
</html>
<!-- END: main -->