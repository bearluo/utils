<?php

define('DS', DIRECTORY_SEPARATOR);
define('BIN_DIR', rtrim(dirname(dirname(__DIR__)), '/\\'));

if (DS == '/')
{
		if(php_uname('s') == 'Linux'){
			define('LUAJIT_BIN', BIN_DIR . '/linux/luajit');
			define('LUAJIT64_BIN', BIN_DIR . '/linux/luajit64');
		}else{
			define('LUAJIT_BIN', BIN_DIR . '/mac/luajit');
			define('LUAJIT64_BIN', BIN_DIR . '/mac/luajit64');
		}
}
else
{
    define('LUAJIT_BIN', BIN_DIR . '\\win32\\luajit.exe');
}

// helper functions

function fetchCommandLineArguments($arg, $options, $minNumArgs = 0)
{
    if (!is_array($arg) || !is_array($options))
    {
        print("ERR: invalid command line arguments");
        return false;
    }

    $config = array();
    $newOptions = array();
    for ($i = 0; $i < count($options); $i++)
    {
        $option = $options[$i];
        $newOptions[$option[0]] = $option;
        $config[$option[1]] = $option[3];
    }
    $options = $newOptions;

    $i = 1;
    while ($i < count($arg))
    {
        $a = $arg[$i];
        if ($a{0} != '-')
        {
            printf("ERR: invalid argument %d: %s", $i, $a);
            return false;
        }

        $a = substr($a, 1);
        if (!isset($options[$a]))
        {
            printf("ERR: invalid argument %d: -%s", $i, $a);
            return false;
        }

        $key = $options[$a][1];
        $num = $options[$a][2];
        $default = $options[$a][3];

        if ($num == 0)
        {
            $config[$key] = true;
        }
        else
        {
            $values = array();
            for ($n = 1; $n <= $num; $n++)
            {
                $values[] = $arg[$i + $n];
            }
            if (count($values) == 1)
            {
                $config[$key] = $values[0];
            }
            else
            {
                $config[$key] = $values;
            }
        }

        $i = $i + $num + 1;
    }

    return $config;
}

function convertConfigValueToString($value)
{
    if (is_null($value))
    {
        return null;
    }
    else if (is_array($value))
    {
        foreach ($value as $k => $v)
        {
            $value[$k] = convertConfigValueToString($v);
        }
    }
    else if (is_string($value))
    {
        return '"' . $value . '"';
    }
    else
    {
        return (string)$value;
    }
}

function dumpConfig($config, $options)
{
    print("config:\n");
    for ($i = 0; $i < count($options); $i++)
    {
        $key = $options[$i][1];
        $value = convertConfigValueToString($config[$key]);
        if ($value != null)
        {
            printf("    %s = %s\n", $key, $value);
        }
    }
    print("\n");
}

function findFiles($dir, array & $files)
{
    $dir = rtrim($dir, "/\\") . DS;
    $dh = opendir($dir);
    if ($dh == false) return;

    while (($file = readdir($dh)) !== false)
    {
		if ($file == '.' || $file == '..' || $file == ".DS_Store" || $file == ".svn")
		{
			continue;
		}

        $path = $dir . $file;
        if (is_dir($path))
        {
            findFiles($path, $files);
        }
        elseif (is_file($path))
        {
            $files[] = $path;
        }
    }
    closedir($dh);
}

function getScriptFileBytecodes($bit, $path, $tmpfile)
{
    if (!file_exists($path))
    {
        printf("ERR: cannot read Lua source file %s\n", $path);
        return false;
    }

    if (file_exists($tmpfile))
    {
        if (!unlink($tmpfile))
        {
            printf("ERR: cannot remove tmp file %s\n", $tmpfile);
            return false;
        }
    }

    @mkdir(pathinfo($tmpfile, PATHINFO_DIRNAME), 0777, true);

	$bin = null;
	if ($bit == '32') {
		$bin = LUAJIT_BIN;
	} else {
		$bin = LUAJIT64_BIN;
	}
	/* -g keep debug info */
	$command = sprintf('%s -bg "%s" "%s"', $bin, $path, $tmpfile);
    passthru($command);

    if (!file_exists($tmpfile))
    {
        printf("ERR: cannot compile file %s\n", $path);
        return false;
    }
    $contents = file_get_contents($tmpfile);
    $contents =  cutDebugInfo($contents);
    return $contents;
}

/** 
* 简化debug 信息中的路径 
* @param $contents 
* @return $contents 
*/
function cutDebugInfo($contents){
    //第4和第5个字节 存储debug信息中lua文件路径长度 + "@" 的长度  debug = “@” + path
    $debugInfoLenBytes = stringToBytes(substr($contents,4,2));
    $debugInfoLen = bytesToShort($debugInfoLenBytes,0,true);
    $debugInfo = substr($contents,6,$debugInfoLen);

    //printf("debugInfo:%d,%d,(%d) %s\n", $debugInfoLenBytes[0],$debugInfoLenBytes[1],$debugInfoLen,$debugInfo);

    $contentsPrefixBytes = stringToBytes(substr($contents,0,5));
    $contentsSuffix = substr($contents,5 + $debugInfoLen + 1);

    $searchStr = "src".DIRECTORY_SEPARATOR;
    
    //查找是否存在src/路径  
    $pos = strpos($debugInfo, "src".DIRECTORY_SEPARATOR);
    //printf("search str \"%s\" result %d\n",$searchStr,$pos);
    if($pos === false){
        //不存在
    }else{
        //存在,则截断src/ 前面的路径字符存
        $debugInfo = "@".substr($debugInfo,$pos); 
        $debugInfoLen = strlen($debugInfo);
        $debugInfoLenBytes = shortToBytes(strlen($debugInfo),true);
        //printf("debugInfo:%d,%d,(%d) %s\n", $debugInfoLenBytes[0],$debugInfoLenBytes[1],$debugInfoLen,$debugInfo);

        //修改debug 信息长度
        $contentsPrefixBytes[4] = $debugInfoLenBytes[0];
        $contentsPrefixBytes[5] = $debugInfoLenBytes[1];

        //拼接完整的数据
        $contents = byteToString($contentsPrefixBytes).$debugInfo.$contentsSuffix;
    }
    return $contents;
}

/** 
* 转换一个String字符串为byte数组 
* @param $str 需要转换的字符串 
* @return $bytes 目标byte数组 
*/
function stringToBytes($string) { 
    $bytes = array(); 
    for($i = 0; $i < strlen($string); $i++){ 
        $bytes[] = ord($string[$i]); 
    } 
    return $bytes; 
} 

/** 
* 将字节数组转化为String类型的数据 
* @param $bytes 字节数组 
* @return 一个String类型的数据 
*/
function byteToString($bytes) { 
    $str = ''; 
    foreach($bytes as $ch) { 
    $str .= chr($ch); 
    } 
    return $str; 
} 


/** 
* 从字节数组中指定的位置读取一个Short类型的数据。 
* @param $bytes 字节数组 
* @param $position 指定的开始位置 
* @param $isBigendian 字节数组是否大端
* @return $val 一个Short类型的数据 
*/
function bytesToShort($bytes, $position,$isBigendian) { 
    $val = 0; 
    if($isBigendian){
        $heigh = ($bytes[$position] & 0xff) << 8;
        $low = $bytes[$position + 1] & 0xff;
    }else{
        $heigh = ($bytes[$position + 1] & 0xff) << 8;
        $low = $bytes[$position] & 0xff; 
    }
    $val = $heigh + $low;
    //printf("bytesToShort %d + %d = %d\n",$heigh,$low,$val);
    return $val; 
} 

/** 
* 转换一个shor字符串为byte数组 
* @param $val short 类型 
* @return $byt 目标byte数组 
* 
*/
function shortToBytes($val,$isBigendian) { 
    $byt = array(); 
    if($isBigendian){
        $byt[0] = ($val >> 8 & 0xff); 
        $byt[1] = ($val & 0xff); 
        //printf("shortToBytes %d * 256 + %d = %d\n",$byt[0],$byt[1],$val);
    }else{
        $byt[0] = ($val & 0xff); 
        $byt[1] = ($val >> 8 & 0xff); 
        //printf("shortToBytes %d + %d * 256 = %d\n",$byt[0],$byt[1],$val);
    }
    
    return $byt; 
} 


