<!-- BEGIN: main -->
<!DOCTYPE html>
<html>
	<head>
		<title>Ring Machine Master Node - Admin Dashboard</title>
		<style type="text/css">
			body {
				font-family: "Georgia", serif;
			}
			
			table {
				margin-top: 30px;
				margin-left: 20px;
			}
			
			td {
				border: 1px solid #000000;
				padding: 5px;
			}
		</style>
	</head>
	<body>
		<h2>Ring Machine Master Node: {HOST}</h2>
		
		<h3>Node Information:</h3>
		<ul>
			<li>Uptime: {UPTIME}</li>
			<li>Cluster workers: {WORKERCOUNT}</li>
			<li>Files: {FILECOUNT}</li>
		</ul>
		
		<h3>Files</h3>
		<a href="?page=upload">Upload a File</a>
		
		<table>
			<tr>
				<td><b>ID</b></td>
				<td><b>Filename</b></td>
				<td><b>Type</b></td>
				<td><b>Size</b></td>
				<td><b>DL Link</b></td>
			</tr>
			<!-- BEGIN: file -->
			<tr>
				<td>{FILEID}</td>
				<td>{FILENAME}</td>
				<td>{FILETYPE}</td>
				<td>{FILESIZE}</td>
				<td><a href="{DOWNURL}">{DOWNTXT}</a></td>
			</tr>
			<!-- END: file -->  
		</table>
				
		</table>
	</body>
</html>
<!-- END: main -->