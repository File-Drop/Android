package com.filedrop;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import jp.develop.common.util.*;

import android.net.*;
import android.os.*;
import android.app.*;
import android.content.*;
import android.content.res.Resources;
import android.util.*;
import android.view.*;
import android.view.ContextMenu.*;
import android.widget.*;
import android.telephony.*;

public class FileDrop extends Activity {

	ActionBar actionBar;
	boolean islistening=false;
	boolean isfileing=false;
	String mydevice;
	SharedPreferences datas;
	SharedPreferences.Editor editor;
	String language;
	boolean isCn;
	Resources resources;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		resources=getResources();
		getOverflowMenu();
		actionBar=getActionBar();
		actionBar.show();
		datas=getPreferences(0);
		editor=datas.edit();
		mydevice=datas.getString("name", "MyAndroid");
		language=Locale.getDefault().getLanguage();
		Log.w("lan", language);
		if (!language.equals("en") && !language.equals("zh")) language="en";
		if (language.equals("zh")) isCn=true;
		else isCn=false;
		showDirectory();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		setIconEnable(menu, true);
		return super.onCreateOptionsMenu(menu);
	}
	
	File[] files=null;
	String nowDirectory="";
	File file;
	File tmpFile;
	
	public void showDirectory() {
		invalidateOptionsMenu();
		actionBar.setTitle(mydevice+" - FileDrop");
		islistening=false;looking=false;
		sendtype=0;
		setContentView(R.layout.show_page);
		TextView textView=(TextView)findViewById(R.id.url);
		if (nowDirectory.equals("")) {
			file=Environment.getExternalStorageDirectory();
			nowDirectory=file.getPath();
		}
		else
			file=new File(nowDirectory);
		files=file.listFiles();
		textView.setText(file.getPath());

		List<HashMap<String, Object>> list=getList(files);
		SimpleAdapter simpleAdapter=new SimpleAdapter(this, list, R.layout.show_file,
				new String[] {"image_view", "name"},new int[]{R.id.image_view,R.id.name});
		ListView listView=(ListView)findViewById(R.id.listView);
		listView.setAdapter(simpleAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (files[arg2].isDirectory()) {
					File[] childFiles=files[arg2].listFiles();
					if (childFiles!=null) {
						nowDirectory=files[arg2].getPath();
						showDirectory();
					}
				}
				else {
					try {
						Intent intent=new Intent();
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.setAction(Intent.ACTION_VIEW);
						String type=getMIMEType(files[arg2]);
						intent.setDataAndType(Uri.fromFile(files[arg2]), type);
						startActivity(intent);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(FileDrop.this, R.string.unabletoopen, 
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> arg0, 
					View arg1, int arg2, long arg3) {
				tmpFile=files[(int)arg3];
				return false;
			}
		});
		this.registerForContextMenu(listView);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu,View v,ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(Menu.NONE,0,0,R.string.menusend);
		menu.add(Menu.NONE,1,1,R.string.change);
		menu.add(Menu.NONE,2,2,R.string.menudetails);
		menu.add(Menu.NONE,3,3,R.string.menudelete);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 0:
				sendtype=2;
				sendfile=tmpFile;
				lookingFor();
				break;
			case 1:changeFileName();
				break;
			case 2:
				boolean canRead=tmpFile.canRead(),canWrite=tmpFile.canWrite();
				boolean isHide=tmpFile.isHidden();
				String absolutePath=tmpFile.getPath();
				long total=getFileSize(tmpFile);
				String size=FormetFileSize(total);
				String s0="";
				if (tmpFile.isDirectory())
					s0+=resources.getString(R.string.details1)+" "+
							(dirNum(tmpFile)-1)+resources.getString(R.string.details2)+" "+
					fileNum(tmpFile)+resources.getString(R.string.details3);
				else {
					s0+=resources.getString(R.string.details4)+" ";
					String tmp=getSuffix(tmpFile);
					if (tmp.equals("") || tmp.length()>=6) {
						s0+=resources.getString(R.string.details5);
					}
					else {
						s0+=tmp.substring(1)+resources.getString(R.string.details6)+tmp+")\n";
					}
				}
				Date date=new Date(tmpFile.lastModified());
				SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
				simpleDateFormat.format(date);
				String s="";
				s=resources.getString(R.string.x1)+tmpFile.getName()+resources.getString(R.string.x2)+s0;
				s+=resources.getString(R.string.x3); 
				if (canRead) s+=resources.getString(R.string.yes); 
					else s+=resources.getString(R.string.no);
				s+=resources.getString(R.string.x4); 
				if (canWrite) s+=resources.getString(R.string.yes);
					else s+=resources.getString(R.string.no);
				s+=resources.getString(R.string.x5);
				if (isHide) s+=resources.getString(R.string.yes); 
					else s+=resources.getString(R.string.no);
				s+=resources.getString(R.string.x6)+absolutePath;
				s+=resources.getString(R.string.x7)+size+" ("+total+resources.getString(R.string.x8);
				s+=resources.getString(R.string.x9)+simpleDateFormat.format(date);
				final String msg=new String(s);
				new AlertDialog.Builder(this).setTitle(R.string.details7).setMessage(msg)
				.setCancelable(true).setPositiveButton(R.string.details8, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						ClipboardManager cpManager=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
						cpManager.setPrimaryClip(ClipData.newPlainText("text", tmpFile.getPath()));
						Toast.makeText(FileDrop.this, R.string.details9, Toast.LENGTH_SHORT).show();
					}
				}).setNegativeButton(R.string.close, null).show();
				break;
			case 3:
				String string=resources.getString(R.string.details10);
				if (tmpFile.isDirectory())
					string=resources.getString(R.string.details11);
				new AlertDialog.Builder(this).setTitle(R.string.details12).setMessage(string).
				setIcon(R.drawable.del).setPositiveButton(R.string.confirm, 
						new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						delFile(tmpFile);
						Toast.makeText(FileDrop.this, R.string.details13, Toast.LENGTH_SHORT).show();
						showDirectory();
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						}
				}).show();
				break;
		}
		return super.onContextItemSelected(item);
	}
	
	void delFile(File file) {
		if (file.isFile()) {
			file.delete();
			return;
		}
		if (file.isDirectory()) {
			File[] fs=file.listFiles();
			for (int i=0;i<fs.length;i++)
				delFile(fs[i]);
			file.delete();
			return;
		}
	}
	
	long getFileSize(File file) {
		long size=0;
		if (file.isDirectory()) {
			File[] ffs=file.listFiles();
			for (int i=0;i<ffs.length;i++)
				size+=getFileSize(ffs[i]);
		}
		else size+=file.length();
		return size;
	}
	
	public String FormetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "K";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "G";
        }
        return fileSizeString;
    }
	
	int fileNum(File file) {
		if (!file.isDirectory()) return 1;
		int num=0;
		File[] ffs=file.listFiles();
		for (int i=0;i<ffs.length;i++)
			num+=fileNum(ffs[i]);
		return num;
	}
	
	int dirNum(File file) {
		if (!file.isDirectory()) return 0;
		int num=1;
		File[] ffs=file.listFiles();
		for (int i=0;i<ffs.length;i++)
			num+=dirNum(ffs[i]);
		return num;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case 0:listening();break;
		case 1:inputString(0);break;
		case 2:newDirectory();break;
		case 3:lookingFor();break;
		case 4:inputString(2);break;
		case 5:Uri uri=Uri.parse("http://www.xiongdianpku.com/products/file-drop");
		Intent intent=new Intent(Intent.ACTION_VIEW, uri);startActivity(intent);break;
		case 8:finish();break;
		case 6:searchingDevice();break;
		case 7:inputString(1);break;
		default:break;
		}
		return true;
	}
	
	
	String end="",selectString;
	public void changeFileName() {
		end=getSuffix(tmpFile);
		final Dialog dialog=new Dialog(this);
		dialog.setContentView(R.layout.jump_page);
		dialog.setTitle(R.string.change);
		dialog.setCancelable(true);
		dialog.show();
		((EditText)dialog.findViewById(R.id.renametext)).setText(tmpFile.getName());
		Button yesbutton=(Button)dialog.findViewById(R.id.change);
		yesbutton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				EditText editText=(EditText)dialog.findViewById(R.id.renametext);
				selectString=editText.getText().toString();
				if (selectString.equals(""))
				{
					Toast.makeText(FileDrop.this, R.string.details14, Toast.LENGTH_SHORT).show();
					return;
				}
				dialog.dismiss();
				String str="";
				str=resources.getString(R.string.x10)+" "+end+" "+resources.getString(R.string.x11);
				int next=selectString.lastIndexOf(".");
				if (next<0 && !end.equals("")) {
					new AlertDialog.Builder(FileDrop.this).setTitle(R.string.hint)
					.setMessage(str).setCancelable(false).setPositiveButton(R.string.confirm, 
							new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							selectString+=end;rename();
						}
					}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							rename();
						}
					}).show();
				}
				else
					rename();
			}
		});
		Button nobutton=(Button)dialog.findViewById(R.id.cancel);
		nobutton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
			}
		});
	}
	
	int sendtype=0;
	String sendString="";
	File sendfile=null;
	public void inputString(int type) {
		
		final Dialog dialog=new Dialog(this);
		dialog.setContentView(R.layout.inputstring_page);
		final int tp=Integer.valueOf(type);
		if (tp==0)
			dialog.setTitle(R.string.details15);
		else if (tp==1)
			dialog.setTitle(R.string.details16);
		else if (tp==2)
			dialog.setTitle(R.string.details17);
		dialog.setCancelable(true);
		dialog.show();
		EditText editText=(EditText)dialog.findViewById(R.id.searchC);
		if (tp!=0) editText.setSingleLine(true);
		if (tp==1)
		{
			editText.setHint(R.string.details18);
		}
		if (tp==2)
		{
			editText.setText(mydevice);
			editText.setSelectAllOnFocus(true);
		}
		Button yesbutton=(Button)dialog.findViewById(R.id.change);
		yesbutton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				EditText editText=(EditText)dialog.findViewById(R.id.searchC);
				selectString=editText.getText().toString();
				if (tp==0 && selectString.equals(""))
				{
					Toast.makeText(FileDrop.this, R.string.details14, Toast.LENGTH_SHORT).show();
					return;
				}
				if (tp==1) {
					Pattern pattern=Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
					if (!pattern.matcher(selectString).find())
					{
						Toast.makeText(FileDrop.this, R.string.details14, Toast.LENGTH_SHORT).show();
						return;
					}
					dialog.dismiss();
					new Thread() {
						public void run() {
							search(selectString, 1);
						}
					}.start();
					return;
				}
				if (tp==2)
				{
					if (selectString.equals(""))
					{
						Toast.makeText(FileDrop.this, R.string.details14, Toast.LENGTH_SHORT).show();
						return;
					}
					dialog.dismiss();
					mydevice=selectString;
					editor.putString("name", mydevice);
					editor.commit();
					Toast.makeText(FileDrop.this, R.string.details19, Toast.LENGTH_SHORT).show();
					actionBar.setTitle(mydevice+" - FileDrop");
					return;
				}
				dialog.dismiss();
				sendString=selectString;
				sendtype=1;
				lookingFor();
				return;
				
			}
		});
		Button nobutton=(Button)dialog.findViewById(R.id.cancel);
		nobutton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
			}
		});		
	}
	
	List<HashMap<String, Object>> deviceList;
	SimpleAdapter deviceSimpleAdapter;
	ListView diviceListView;
	boolean looking,searching;
	private class Devicemsg {
		String ip,name,type;
		Devicemsg(String _ip,String _name,String _type) {
			ip=_ip;name=_name;type=_type;
		}
	}
	public void lookingFor() {
		actionBar.setTitle(R.string.details20);
		deviceList=new ArrayList<HashMap<String,Object>>();
		deviceList.clear();
		looking=true;
		invalidateOptionsMenu();
		deviceSimpleAdapter=new SimpleAdapter(this, deviceList, R.layout.show_device,
				new String[] {"devicename", "deviceid","devicetype","deviceip"},
				new int[]{R.id.devicename,R.id.deviceid,R.id.devicetype,R.id.deviceip});
		ListView listView=new ListView(this);
		listView.setAdapter(deviceSimpleAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (sendtype==0) {
					new AlertDialog.Builder(FileDrop.this).setTitle(R.string.hint).setCancelable(true)
					.setMessage(R.string.details21)
					.setPositiveButton(R.string.confirm, null).show();
					return;
				}
				if (sendtype==1) {
					final String targetIp=new String((String)deviceList.get(arg2).get("deviceip"));
					String xString=resources.getString(R.string.x12)+deviceList.get(arg2).get("devicename")+resources.getString(R.string.x13);
					final String tString=new String(xString);
					new AlertDialog.Builder(FileDrop.this).setTitle(R.string.details22).setCancelable(true)
					.setMessage(tString)
					.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							sendText(targetIp);
						}
					}).setNegativeButton(R.string.cancel, null).show();
					return;
				}
				if (sendtype==2) {
					final Devicemsg sendDevice=new Devicemsg((String)deviceList.get(arg2).get("deviceip"),
							(String)deviceList.get(arg2).get("devicename"),(String)deviceList.get(arg2).get("devicetype"));
					String xString=resources.getString(R.string.x14)+tmpFile.getName()+resources.getString(R.string.x15)+deviceList.get(arg2).get("devicename")+resources.getString(R.string.x13);
					final String tString=new String(xString);
					new AlertDialog.Builder(FileDrop.this).setTitle(R.string.details22).setCancelable(true)
					.setMessage(tString)
					.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							sendFile(sendfile, sendDevice);
						}
					}).setNegativeButton(R.string.cancel, null).show();
					return;
				}
			}
		});
		setContentView(listView);
		searchingDevice();
	} 
	
	void searchingDevice() {
		searching=true;
		deviceList.clear();
		deviceSimpleAdapter.notifyDataSetChanged();
		Toast.makeText(this, R.string.details23, Toast.LENGTH_SHORT).show();
		new Thread() {
			public void run() {
				Looper mainLooper=getMainLooper();
				EventHandler mainHandler=new EventHandler(mainLooper);
				Pattern pattern=Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
				Enumeration<NetworkInterface> netInterfaces = null;
				try {
					netInterfaces = NetworkInterface.getNetworkInterfaces();
					while (netInterfaces.hasMoreElements()) {
						NetworkInterface ni = netInterfaces.nextElement();
						Enumeration<InetAddress> ips = ni.getInetAddresses();
						while (ips.hasMoreElements()) {
							String tmpString=ips.nextElement().getHostAddress();
							if (!tmpString.equals("127.0.0.1") && 
									pattern.matcher(tmpString).find())
								search_ip(tmpString);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				mainHandler.sendMessage(mainHandler.obtainMessage(0,resources.getString(R.string.details24)));
				searching=false;
			}
		}.start();
	}
	
	void sendText(String sendIp) {
		final String tmpIp=new String(sendIp);
		new Thread() {
			public void run() {
				EventHandler mainHandler=new EventHandler(Looper.getMainLooper());
				try {
					Socket socket=null;
					OutputStream outputStream=null;
					socket=new Socket(tmpIp, 12345);
					outputStream=socket.getOutputStream();
					PrintStream printStream=new PrintStream(outputStream);
					InputStream inputStream=socket.getInputStream();
					BufferedReader bf=new BufferedReader(new InputStreamReader(inputStream));
					printStream.write("send text".getBytes());
					printStream.flush();
					char[] xx=new char[100000];
					bf.read(xx);
					String msg=new String(xx).trim();
					if (msg.equals("ok")) {
						String mymsg=mydevice+"\n"+sendString;
						int length=mymsg.getBytes().length;
						for (int i=0;i<=3;i++)
						{
							byte x=(byte)((length>>(8*i))&0xFF);
							printStream.write(x);
						}
						
						Log.w("send text length", length+"");
						
						printStream.write(mymsg.getBytes());
						printStream.flush();
					}
					else if (msg.equals("busy")) {
						mainHandler.sendMessage(mainHandler.obtainMessage(3));
						return;
					}
					bf.read(xx);
					msg=new String(xx).trim();
					if (msg.equals("ok"))
						mainHandler.sendMessage(mainHandler.obtainMessage(4));
				} catch (IOException e) {
					mainHandler.sendMessage(mainHandler.obtainMessage(3));
				}
			}
		}.start();
	}
	
	void search_ip(String hereip) {
		int dotindex=hereip.lastIndexOf(".");
		final String frontString=hereip.substring(0, dotindex);
		for (int i=0;i<=255;i++)
		{
			final int x=Integer.valueOf(i);
			new Thread() {
				public void run() {
					search(frontString+"."+x,0);
				}
			}.start();
		}
	}
	
	void search(String targetIp,int method) {
		EventHandler mainHandler=new EventHandler(Looper.getMainLooper());
		try {
			Socket socket=null;
			OutputStream outputStream=null;
			socket=new Socket(targetIp, 12345);
			//if (method==1)
			//socket.setSoTimeout(5000);
			outputStream=socket.getOutputStream();
			PrintWriter printWriter=new PrintWriter(outputStream);
			printWriter.print("ask for info");
			printWriter.flush();
			InputStream inputStream=socket.getInputStream();
			BufferedReader bf;
			bf=new BufferedReader(new InputStreamReader(inputStream));
			char[] xx=new char[1000];
			bf.read(xx);
			String msg=new String(xx).trim();
			socket.close();
			int index=msg.lastIndexOf(":");
			if (index!=-1) {
				String type=msg.substring(index+1, msg.length());
				String front=msg.substring(0, index);
				if (!type.equals("android") && !type.equals("iphone") &&
						!type.equals("win") && !type.equals("mac") &&
						!type.equals("ipad"))
					type="unknown";
				mainHandler.sendMessage(mainHandler.obtainMessage(2, method,0,new Devicemsg(targetIp, front, type)));
			}
		} catch (IOException e) {
			if (method==1)
				mainHandler.sendMessage(mainHandler.obtainMessage(0,resources.getString(R.string.details25)+targetIp));
		}
	}
	
	public void rename() {
		tmpFile.renameTo(new File(tmpFile.getParentFile().getPath()+"/"+selectString));
		Toast.makeText(this, R.string.details26, Toast.LENGTH_SHORT).show();
		showDirectory();
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		if (looking) {
			menu.add(Menu.NONE, 6, 6, R.string.refresh).setIcon(R.drawable.reload)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menu.add(Menu.NONE, 7, 7, R.string.add).setIcon(R.drawable.add)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			return true;
		}
		if (islistening) return true;
		menu.add(Menu.NONE, 0, 0, "").setIcon(R.drawable.monitor)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(Menu.NONE, 1, 1, "").setIcon(R.drawable.edit)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(Menu.NONE, 2, 2, "  "+resources.getString(R.string.newf)).setIcon(R.drawable.newdir);
		menu.add(Menu.NONE, 3, 3, "  "+resources.getString(R.string.search)).setIcon(R.drawable.find);
		menu.add(Menu.NONE, 4, 4, "  "+resources.getString(R.string.set)).setIcon(R.drawable.setting);
		menu.add(Menu.NONE, 5, 5, "  "+resources.getString(R.string.help)).setIcon(R.drawable.help);
		menu.add(Menu.NONE, 8, 8, "  "+resources.getString(R.string.exit)).setIcon(R.drawable.exit);
		return true;
	}
	
	public void newDirectory() {
		final Dialog dialog=new Dialog(this);
		dialog.setContentView(R.layout.new_directory_page);
		dialog.setTitle(R.string.newdir);
		dialog.setCancelable(true);
		dialog.show();
		Button yesbutton=(Button)dialog.findViewById(R.id.change);
		yesbutton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				EditText editText=(EditText)dialog.findViewById(R.id.newtext);
				selectString=editText.getText().toString();
				if (selectString.equals(""))
				{
					Toast.makeText(FileDrop.this, R.string.details14, Toast.LENGTH_SHORT).show();
					return;
				}
				if (selectString.contains("/") || selectString.contains("\\")
						|| selectString.contains(":") || selectString.contains("*")
						|| selectString.contains("\"") || selectString.contains("<")
						|| selectString.contains(">") || selectString.contains("|"))
				{
					Toast.makeText(FileDrop.this, 
							resources.getString(R.string.details27)+"\n         / \\ : * \" < > |",
							Toast.LENGTH_SHORT).show();
					return;
				}
				File newfile=new File(nowDirectory+"/"+selectString);
				if (newfile.exists()) {
					Toast.makeText(FileDrop.this, R.string.details28, 
							Toast.LENGTH_SHORT).show();
					return;
				}
				dialog.dismiss();
				newfile.mkdir();
				nowDirectory=newfile.getPath();
				showDirectory();
			}
		});
		Button nobutton=(Button)dialog.findViewById(R.id.cancel);
		nobutton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
			}
		});
		
	}
	
	List<HashMap<String, Object>> getList(File[] files) {
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File x1,File x2) {
				String y1=x1.getName(),y2=x2.getName();
				return y1.compareToIgnoreCase(y2);
			}
		});
		List<HashMap<String, Object>> list=new ArrayList<HashMap<String,Object>>();
		for (int i=0;i<files.length;i++) {
			HashMap<String, Object> hashMap=new HashMap<String, Object>();
			if (files[i].isDirectory()) {
				hashMap.put("image_view", R.drawable.directory);
			}
			else {
				String suffix=getSuffix(files[i]).toLowerCase(Locale.getDefault());
				if (suffix.equals(".mp3")) hashMap.put("image_view", R.drawable.mpthree);
				else if (suffix.equals(".mp4")) 
					hashMap.put("image_view", R.drawable.mpfour);
				else if (suffix.equals(".avi")) hashMap.put("image_view", R.drawable.avi);
				else if (suffix.equals(".bmp")) hashMap.put("image_view", R.drawable.bmp);
				else if (suffix.equals(".doc") || suffix.equals(".docx"))
					hashMap.put("image_view", R.drawable.doc);
				else if (suffix.equals(".gif")) hashMap.put("image_view", R.drawable.gif);
				else if (suffix.equals(".htm") || suffix.equals(".html"))
					hashMap.put("image_view", R.drawable.html);
				else if (suffix.equals(".jpg") || suffix.equals(".jpeg"))
					hashMap.put("image_view", R.drawable.jpg);
				else if (suffix.equals(".mov")) hashMap.put("image_view", R.drawable.mov);
				else if (suffix.equals(".mpg")) hashMap.put("image_view", R.drawable.mpg);
				else if (suffix.equals(".pdf")) hashMap.put("image_view", R.drawable.pdf);
				else if (suffix.equals(".png")) hashMap.put("image_view", R.drawable.png);
				else if (suffix.equals(".ppt") || suffix.equals(".pptx"))
					hashMap.put("image_view", R.drawable.ppt);
				else if (suffix.equals(".rar")) hashMap.put("image_view", R.drawable.rar);
				else if (suffix.equals(".txt")) hashMap.put("image_view", R.drawable.txt);
				else if (suffix.equals(".swf")) hashMap.put("image_view", R.drawable.swf);
				else if (suffix.equals(".wav")) hashMap.put("image_view", R.drawable.wav);
				else if (suffix.equals(".wma")) hashMap.put("image_view", R.drawable.wma);
				else if (suffix.equals(".wmv")) hashMap.put("image_view", R.drawable.wmv);
				else if (suffix.equals(".zip")) hashMap.put("image_view", R.drawable.zip);
				else hashMap.put("image_view", R.drawable.file);
			}
			hashMap.put("name", files[i].getName());
			list.add(hashMap);
		}
		return list;
	}
	
	class RequestingTask extends AsyncTask<Integer, Integer, Boolean> {
		ServerSocket serverSocket;
		Context context;
		EventHandler eventHandler;
		
		public RequestingTask() {
			eventHandler=new EventHandler(Looper.getMainLooper());
			try {
				serverSocket=new ServerSocket();
				serverSocket.setReuseAddress(true);
				serverSocket.bind(new InetSocketAddress(12345));
				serverSocket.setSoTimeout(1000);
			} catch (IOException e) {serverSocket=null;
			Log.w("err", "error");
				eventHandler.sendMessage(eventHandler.obtainMessage(0, R.string.details29));
				Log.w("error", e.getMessage());
			}
		}
		protected void onPreExecute() {
			Log.w("test", "start listening..");
		}
		protected Boolean doInBackground(Integer... arg0) {
			while (true) {
				try {
					final Socket socket=serverSocket.accept();
					BufferedInputStream bis=new BufferedInputStream(socket.getInputStream());
					final PrintWriter pw=new PrintWriter(socket.getOutputStream());
					byte[] xx=new byte[1000];
					bis.read(xx);
					String msg=new String(xx).trim();
					if (msg.equals("ask for info")) {
						String remoteIp=socket.getInetAddress().toString();
						if (remoteIp.charAt(0)=='/')
							remoteIp=remoteIp.substring(1);
						Log.w("find", remoteIp);
						String xString=resources.getString(R.string.x16)+remoteIp+resources.getString(R.string.x17);
						eventHandler.sendMessage(eventHandler.obtainMessage(0, 
								xString));
						pw.write(mydevice+":android");
						pw.flush();
						socket.close();
					}
					else if (msg.equals("send text")) {
						final GetMessage getMessage=new GetMessage(socket, bis, pw);
						new Thread() {
							public void run() {
								getMessage.getMsg();
							}
						}.run();
					}
					else if (msg.equals("send file")) {
						if (isfileing || phoneInUse())
						{
							pw.write("busy");
							pw.flush();
							socket.close();
							continue;
						}
						pw.write("ok");
						pw.flush();
						byte[] bb=new byte[4];
						bis.read(bb);
						int u=(int)((((bb[3]&0xff)<<24)|((bb[2]&0xff)<<16
								|((bb[1]&0xff)<<8)|((bb[0]&0xff)))));
						Log.w("length", u+"");
						byte[] input=new byte[u];
						bis.read(input);
						Object[] fs=AmfUtil.decode(input, Object[].class);
						Log.w("objectlength", fs.length+"");
						GetFileTask getFileTask=new GetFileTask(socket, bis, pw, fs);
						eventHandler.sendMessage(eventHandler.obtainMessage(6, getFileTask));
					}
				} catch (Exception e) {
					if (isCancelled()) {
						try {
							serverSocket.close();
						} catch (IOException ee) {}
						return null;
					}
				}
			} 
		}
		@Override
		protected void onCancelled() {
			try {
				serverSocket.close();
			} catch (IOException e) {}
			super.onCancelled();
		}
		
	}
	
	RequestingTask requestingTask;
	public void listening() {
		invalidateOptionsMenu();
		islistening=true;
		setContentView(R.layout.listen_page);
		((TextView)findViewById(R.id.textView1)).setText(R.string.details30);
		((TextView)findViewById(R.id.textView2)).setText(R.string.details31);
		Pattern pattern=Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
		Enumeration<NetworkInterface> netInterfaces = null;
		try {
			netInterfaces = NetworkInterface.getNetworkInterfaces();
			while (netInterfaces.hasMoreElements()) {
				NetworkInterface ni = netInterfaces.nextElement();
				Enumeration<InetAddress> ips = ni.getInetAddresses();
				while (ips.hasMoreElements()) {
					String tmpString=ips.nextElement().getHostAddress();
					if (!tmpString.equals("127.0.0.1") && 
							pattern.matcher(tmpString).find())
						string+=tmpString+"\n";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		((TextView)findViewById(R.id.textView3)).setText(string);

		requestingTask=new RequestingTask();
		requestingTask.execute();
	}
	
	class GetFileTask extends AsyncTask<Object, Object, Boolean> {
		private Socket socket;
		private BufferedInputStream bf;
		private PrintWriter pw;
		EventHandler eventHandler;
		private Object[] files;
		ProgressDialog progressDialog;
		Context context;
		
		public GetFileTask(Socket socket,BufferedInputStream bf,PrintWriter pw,Object[] ob) {
			// TODO Auto-generated constructor stub
			this.socket=socket;this.bf=bf;this.pw=pw;this.files=ob;
			Log.w("getFIle", "created!");
		}
		
		void setContext(Context context) {
			this.context=context;
		}
		
		protected void onPostExecute(Boolean b) {
			progressDialog.dismiss();
			isfileing=false;
		}
		
		protected void onProgressUpdate(Object... values) {
			long hr=(Long)values[0],tr=(Long)values[1];
			String msg="正在接收第"+values[2]+"个文件\n\n当前文件:\n"+((File)values[3]).getName()
					+" ("+FormetFileSize((Long)values[4])+")\n\n已经接收 ";
			if (!isCn) {
				int x=(Integer)values[2];
				String sf="th";
				if (x%10==1 && x%100!=11) sf="st";
				if (x%10==2 && x%100!=12) sf="nd";
				if (x%10==3 && x%100!=13) sf="rd";
				msg="Recieving "+x+sf+" file:\n"+((File)values[3]).getName()
						+" ("+FormetFileSize((Long)values[4])+")\n\n";
			}
			msg+=hr+"B/"+tr+"B\n("+FormetFileSize(hr)+"/"+FormetFileSize(tr)+")";
			progressDialog.setMessage(msg);
			progressDialog.setTitle(resources.getString(R.string.details32)+(String)files[0]);
			progressDialog.setProgress((int)((hr+0.0)/tr*100));
		}
		
		protected void onPreExecute() {
			eventHandler=new EventHandler(Looper.getMainLooper());
			progressDialog=new ProgressDialog(context);
			progressDialog.setTitle(resources.getString(R.string.details32)+" "+(String)files[0]);
			progressDialog.setMessage(resources.getString(R.string.details33));
			progressDialog.setCancelable(false);
			progressDialog.setButton(ProgressDialog.BUTTON_NEUTRAL, resources.getString(R.string.cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					GetFileTask.this.cancel(true);
					
				}
			});
			progressDialog.setMax(100);
			progressDialog.setProgress(0);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.show();
			Log.w("test", "before do");
		}
		
		@SuppressWarnings("unchecked")
		protected Boolean doInBackground(Object... arg0) {
			try {
				Log.w("test", "do");
				isfileing=true;
				byte[]bb=new byte[8];
				bf.read(bb);
				long u=((((long)bb[6]&0xff)<<48)|(((long)bb[7]&0xff)<<56)|
						(((long)bb[5]&0xff)<<40)|
						(((long)bb[4]&0xff)<<32)|(((long)bb[3]&0xff)<<24)|
						(((long)bb[2]&0xff)<<16)|
						(((long)bb[1]&0xff)<<8)|(long)(bb[0]&0xff));
				Log.w("u=", u+"");
				double x=Double.longBitsToDouble(u);
				long length=(long) x;
				Log.w("total length", length+"");
				pw.write("ok" +"");pw.flush();
				
				long hasRecieved=0;
				int nowRecieve=0;
				while (true) {
					byte[] filemsglength=new byte[4];
					int isok=bf.read(filemsglength);
					if (isok<=2) break;
					int y=(int)((((filemsglength[3]&0xff)<<24)|((filemsglength[2]&0xff)
							<<16|((filemsglength[1]&0xff)<<8)|(filemsglength[0]&0xff))));
					nowRecieve++;
					Log.w("nowrecieve: "+nowRecieve, y+"");
					byte[] filemsg=new byte[(int)y];
					bf.read(filemsg);
					HashMap<String, String> hashMap=(HashMap<String, String>)AmfUtil.decode(filemsg);
					Log.w("filepath:"+nowRecieve, hashMap.get("path"));
					Log.w("filesize:"+nowRecieve, hashMap.get("size"));
					
					if (isCancelled()) {
						socket.close();
						String mm="接收已取消，已经接收"+FormetFileSize(hasRecieved);
						if (!isCn) mm="Process canceled, "+FormetFileSize(hasRecieved)+" received";
						eventHandler.sendMessage(eventHandler.obtainMessage(0, mm));
						return true;
					}
					
					pw.write("ok");pw.flush();
					
					long size=Long.parseLong(hashMap.get("size"));
					String path=hashMap.get("path");
					int dot=path.lastIndexOf("/");
					if (dot>=0)
					{
						String dir=path.substring(0, dot);
						String dirpath=nowDirectory+"/"+dir;
						Log.w("file", dirpath);
						File dirFile=new File(dirpath);
						dirFile.mkdirs();
					}
					path=nowDirectory+"/"+path;
					File file=new File(path);
					file.createNewFile();
					FileOutputStream fileOutputStream=new FileOutputStream(file);
					byte[] bts=new byte[1024000];
					long count=0;
					publishProgress(hasRecieved,length,nowRecieve,file,size);
					while (true) {
						int actuallyread=bf.read(bts);
						hasRecieved+=actuallyread;
						fileOutputStream.write(bts, 0, actuallyread);
						count+=actuallyread;
						if (count>=size) break;
						bts=new byte[1024000];
						publishProgress(hasRecieved,length,nowRecieve,file,size);
						if (isCancelled()) {
							fileOutputStream.close();
							socket.close();
							String mm="接收已取消，已经接收"+FormetFileSize(hasRecieved);
							if (!isCn) mm="Process canceled, "+FormetFileSize(hasRecieved)+" received";
							eventHandler.sendMessage(eventHandler.obtainMessage(0, mm));
							return true;
						}
						
					}
					fileOutputStream.close();
					pw.write("ok");pw.flush();
				}
				eventHandler.sendMessage(eventHandler.obtainMessage(7));
				pw.close();
				bf.close();
				socket.close();
				return true;
			}
			catch (IOException e) {
				Log.w("get file error", e.getMessage());
				eventHandler.sendMessage(eventHandler.obtainMessage(0, R.string.details34));
				progressDialog.dismiss();
				try {
					socket.close();
					bf.close();
				} catch (IOException ee) {}
			}
			return true;
		}
	}
	
	private class GetMessage {
		final private Socket socket;
		final BufferedInputStream bf;
		final PrintWriter pw;
		public GetMessage(Socket socket,BufferedInputStream bf,PrintWriter pw) {
			this.socket=socket;
			this.bf=bf;
			this.pw=pw;
		}
		
		public void getMsg() {
			try {
				Looper mainLooper=getMainLooper();
				EventHandler mainHandler=new EventHandler(mainLooper);
				pw.write("ok");
				pw.flush();
				String getmsg="";
				byte[] bb=new byte[4];
				bf.read(bb);
				int u=(int)((((bb[3]&0xff)<<24)|((bb[2]&0xff)<<16
						|((bb[1]&0xff)<<8)|((bb[0]&0xff)))));
				BufferedReader bff=new BufferedReader(new InputStreamReader(bf));
				Log.w("length", u+"");
				while (u>0) {
					int tx=bff.read();
					String ttx=new String(Character.toChars(tx));
					u-=ttx.getBytes().length;
					Log.w("u", "u="+u);
					getmsg+=ttx;
				}
				String tmpString=getmsg;
				mainHandler.sendMessage(mainHandler.obtainMessage(1, 0, 0, tmpString));
				pw.write("ok");
				pw.flush();
			} catch (IOException e) {
				isfileing=false;
			}
			finally {
				try {
					socket.close();
				} catch (Exception e) {}
			}
		}
	}
	
	class EventHandler extends Handler {
		public EventHandler() {
			super();
		}
		public EventHandler(Looper looper) {
			super(looper);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message message) {
			super.handleMessage(message);
			switch (message.what) {
			case 0:
				Toast.makeText(FileDrop.this, (String)message.obj, Toast.LENGTH_SHORT).show();
				break;
			case 1:
				String receive=(String)message.obj;
				int index=receive.indexOf("\n");
				final String device=new String(receive.substring(0,index));
				final String text=new String(receive.substring(index+1, receive.length()));
				String xString="您已经收到"+device+"发来的文本：\n\n"+text+"\n\n是否复制到剪切板？";
				if (!isCn)
					xString="Received text from "+device+"\n\n"+text+"\n\nCopy to the clipboard?";
				final String tString=new String(xString);
				Toast.makeText(FileDrop.this, "FileDrop: "+resources.getString(R.string.details35), Toast.LENGTH_SHORT).show();
				new AlertDialog.Builder(FileDrop.this).setTitle("您已收到文本！")
				.setMessage(tString)
				.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
				
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						ClipboardManager cpManager=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
						cpManager.setPrimaryClip(ClipData.newPlainText("text", text));
						Toast.makeText(FileDrop.this, R.string.details36, Toast.LENGTH_SHORT).show();
					}
				}).setCancelable(true).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
					}	
				}).show();
				break;
			case 2:
				Devicemsg devicemsg=(Devicemsg)message.obj;
				HashMap<String, Object> hashMap=new HashMap<String, Object>();
				if (devicemsg.type.equals("win")) devicemsg.type="windows";
				hashMap.put("devicename", devicemsg.name);
				hashMap.put("deviceip", devicemsg.ip);
				hashMap.put("devicetype", devicemsg.type);
				hashMap.put("deviceid", "");
				deviceList.add(hashMap);
				deviceSimpleAdapter.notifyDataSetChanged();
				if (message.arg1==1)
					Toast.makeText(FileDrop.this, resources.getString(R.string.details37)+devicemsg.ip, Toast.LENGTH_SHORT).show();
				break;
			case 3:
				new AlertDialog.Builder(FileDrop.this).setTitle(R.string.fail)
				.setCancelable(true).setMessage(R.string.details38)
				.setPositiveButton(R.string.confirm, null).show();
				break;
			case 4:
				new AlertDialog.Builder(FileDrop.this).setTitle(R.string.success)
				.setCancelable(true).setMessage(R.string.details39)
				.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						ClipboardManager cpManager=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
						cpManager.setPrimaryClip(ClipData.newPlainText("text", sendString));
						Toast.makeText(FileDrop.this, R.string.details36, Toast.LENGTH_SHORT).show();
					}
				}).setNegativeButton(R.string.cancel, null).show();
				sendString="";
				sendtype=0;
				showDirectory();
				break;
			case 5:
				new AlertDialog.Builder(FileDrop.this).setTitle(R.string.success)
				.setCancelable(true).setMessage(R.string.details40)
				.setPositiveButton(R.string.confirm, null).show();
				sendfile=null;
				sendtype=0;
				isfileing=false;
				showDirectory();
				break;
			case 6:
				Log.w("getMessage", "Get message!");
				final GetFileTask getFileTask=(GetFileTask)message.obj;
				Object[] data=getFileTask.files;
				final String devicename=new String((String)data[0]);
				Log.w("name", devicename);
				final int l=data.length-1;
				Toast.makeText(FileDrop.this, "FileDrop: "+resources.getString(R.string.details41), Toast.LENGTH_SHORT).show();
				String files=devicename+" 向您发送文件\n（共计 "+l+" 个文件或文件夹）\n\n";
				if (!isCn)
					files=devicename+" is sending files to you\n("+l+" files or folders in total)\n\n";
				for (int j=1;j<=l;j++) {
					if (!(data[j] instanceof HashMap<?, ?>)) {
						new AlertDialog.Builder(FileDrop.this).setTitle(R.string.error)
						.setCancelable(true).setMessage(R.string.details42)
						.setNegativeButton(R.string.confirm, null).show();
						try {
							getFileTask.bf.close();
							getFileTask.socket.close();
						} catch (IOException e) {}
						return;
					}
					HashMap<String, Object> mp=(HashMap<String, Object>)data[j];
					files+=(String)mp.get("name")+"    ";
					Log.w("files", files);
					if ((Boolean)mp.get("isD"))
					{
						if (isCn)
							files+="(文件夹)";
						else files+="(Folder)";
					}
					else files+="("+FormetFileSize(Long.parseLong((String)mp.get("size")))+")";
					files+="\n";
				}
				if (isCn)
					files+="\n当前目录："+nowDirectory+"\n是否确认接收到该目录下？";
				else 
					files+="\nCurrent directory: "+nowDirectory+"\nConfirm to accept files to this folder?";
				final String msg=new String(files);
				new AlertDialog.Builder(FileDrop.this).setTitle(R.string.details43)
				.setMessage(msg).setPositiveButton(R.string.receive, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						getFileTask.pw.write("ok");
						getFileTask.pw.flush();
						getFileTask.setContext(FileDrop.this);
						getFileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					}
				}).setNegativeButton(R.string.refuse, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						getFileTask.pw.write("reject");
						getFileTask.pw.flush();
						Toast.makeText(FileDrop.this, R.string.details44, Toast.LENGTH_SHORT).show();
						try {
							getFileTask.bf.close();							
							getFileTask.socket.close();
						}catch (IOException e) {}
					}
				}).show();
				break;
			case 7:
				new AlertDialog.Builder(FileDrop.this).setTitle(R.string.success)
				.setCancelable(true).setMessage(R.string.details45)
				.setPositiveButton(R.string.confirm, null).show();
				sendfile=null;
				sendtype=0;
				isfileing=false;
				break;
			default:
				break;
			}
		}
	}
	
	void sendFile(File file,Devicemsg devicemsg) {
		SendFileTask sendFileTask=new SendFileTask(file, devicemsg, this);
		sendFileTask.execute();
	}
	
	class SendFileTask extends AsyncTask<Object, Object, Boolean> {
		Socket socket;
		EventHandler eventHandler;
		File file;
		Devicemsg devicemsg;
		Context context;
		ProgressDialog progressDialog;
		long length,hasSent;
		int type;
		String fileSize;
		int fileNum;
		
		public SendFileTask(File file,Devicemsg devicemsg,Context _context) {
			this.file=file;
			this.devicemsg=devicemsg;
			context=_context;
			length=getFileSize(file);
			fileNum=fileNum(file);
			hasSent=0;
			if (length<1024) type=0;
			else if (length<1024*1024) type=1;
			else type=2;
			eventHandler=new EventHandler(Looper.getMainLooper());
			progressDialog=new ProgressDialog(context);
			progressDialog.setCancelable(false);
			progressDialog.setButton(ProgressDialog.BUTTON_NEUTRAL, resources.getString(R.string.cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					SendFileTask.this.cancel(true);
					
				}
			});
			progressDialog.setMax(100);
			fileSize=resources.getString(R.string.details46)+FormetFileSize(length)+"\n";
			progressDialog.setProgress(0);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage(fileSize+resources.getString(R.string.details48));
			progressDialog.setTitle(resources.getString(R.string.details47)+" "+devicemsg.name);
		}
		
		protected void onPreExecute() {
			progressDialog.show();
			Log.w("test", "start sending file..");
		}
		
		protected Boolean doInBackground(Object... arg0) {
			
			
			//publishProgress(1,(int)1/102400+1,file);
			try {
				socket=new Socket(devicemsg.ip, 12345);
				
				OutputStream outputStream=null;
				outputStream=socket.getOutputStream();
				PrintStream printStream=new PrintStream(outputStream, false);
				//PrintWriter printWriter=new PrintWriter(outputStream);
				InputStream inputStream=socket.getInputStream();
				BufferedReader bf=new BufferedReader(new InputStreamReader(inputStream));
				//printWriter.write("send file");
				//printWriter.flush();
				printStream.write("send file".getBytes());
				printStream.flush();
				char[] xx=new char[100000];
				bf.read(xx);
				String msg=new String(xx).trim();
				
				if (msg.equals("busy")) {
					eventHandler.sendMessage(eventHandler.obtainMessage(3));
					socket.close();
					return true;
				}
				if (msg.equals("ok")) {
					isfileing=true;
					
					Object[] sendMsg=new Object[2];
					sendMsg[0]=mydevice;
					HashMap<String, Object> hashMap=new HashMap<String, Object>();
					hashMap.put("name", file.getName());
					hashMap.put("isD", file.isDirectory());
					if (file.isDirectory()) hashMap.put("size", "0");
					else hashMap.put("size", file.length()+"");
					sendMsg[1]=hashMap;

					byte[] bt=AmfUtil.encode(sendMsg);
					int btl=bt.length;
					byte[] tt=new byte[4];
					for (int i=0;i<=3;i++)
					{
						byte x=(byte)((btl>>(8*i))&0xFF);
						//printWriter.write(x);
						tt[i]=x;
						//printStream.write(x);
						String tmp=Integer.toHexString(x).toUpperCase(Locale.getDefault());
						if (tmp.length()==1) tmp="0"+tmp;
						Log.w("sent", tmp);
					}
					printStream.write(tt);
					//for (int i=0;i<btl;i++)
						//printWriter.write((byte)((int)bt[i]&0xFF));
						printStream.write(bt);
					//printWriter.flush();
						printStream.flush();
						if (isCancelled()) {
							printStream.close();
							socket.close();
							String mm="文件发送取消，已发送"+FormetFileSize(hasSent);
							if (!isCn) mm="File transport canceled, "+FormetFileSize(hasSent)+" bytes sent";
							eventHandler.sendMessage(eventHandler.obtainMessage(0, 
									mm));
							return true;
						}
					xx=new char[1000];
					bf.read(xx);
					msg=new String(xx).trim();
					
					if (msg.equals("reject")) {
						eventHandler.sendMessage(eventHandler.obtainMessage(0,resources.getString(R.string.details49)));
						socket.close();
						return true;
					}
					if (!msg.equals("ok")) {
						socket.close();
						eventHandler.sendMessage(eventHandler.obtainMessage(0, resources.getString(R.string.details50)));
						return true;
					}
					
					byte[] sized=toLH((double)length);
					for (int i=0;i<=7;i++)
					{
						String tmp=Integer.toHexString((int)sized[i]&0xFF).toUpperCase(Locale.getDefault());
						if (tmp.length()==1) tmp="0"+tmp;
						Log.w("sized", tmp);
						//printWriter.write((byte)((int)sized[i]&0xFF));
					}
					printStream.write(sized);
					printStream.flush();
			//		printWriter.flush();
					
					xx=new char[1000];
					bf.read(xx);
					msg=new String(xx).trim();
					Log.w("msg", msg);
					if (!msg.equals("ok")) {
						socket.close();
						eventHandler.sendMessage(eventHandler.obtainMessage(0, resources.getString(R.string.details50)));
						return true;
					}	
					ArrayList<File> arrayList=getArrayList(file);
					Log.w("arraylist", arrayList.size()+"");
					for (int i=0;i<arrayList.size();i++) {
						if (isCancelled()) {
							printStream.close();
							socket.close();
							String mm="文件发送取消，已发送"+FormetFileSize(hasSent);
							if (!isCn) mm="File transport canceled, "+FormetFileSize(hasSent)+" bytes sent";
							eventHandler.sendMessage(eventHandler.obtainMessage(0, 
									mm));
							return true;
						}
						File tmpFile=arrayList.get(i);
						HashMap<String, String> fileMsg=new HashMap<String, String>();
						fileMsg.put("size", Long.toString(tmpFile.length()));
						fileMsg.put("path", getRelativePath(file, tmpFile));
						Log.w("ff", Long.toString(tmpFile.length())+"\n"+getRelativePath(file, tmpFile));
						Object[] p1=new Object[1];
						p1[0]=fileMsg;
						byte[] btt=AmfUtil.encode(fileMsg);
						btl=btt.length;
						Log.w("total length", btl+"");
						byte[] tmobyte=new byte[4];
						for (int j=0;j<=3;j++)
						{
							byte x=(byte)((btl>>(8*j))&0xFF);
							tmobyte[j]=x;
						}
						printStream.write(tmobyte);
						printStream.write(btt);
						printStream.flush();
						xx=new char[1000];
						bf.read(xx);
						msg=new String(xx).trim();
						Log.w("file size?", msg);
						if (!msg.equals("ok")) {
							socket.close();
							eventHandler.sendMessage(eventHandler.obtainMessage(0, resources.getString(R.string.details50)));
							return true;
						}
						//lasttime=nowtime=System.currentTimeMillis();
						publishProgress(1,i+1,tmpFile);
						
						FileInputStream fileInputStream=new FileInputStream(tmpFile);
						byte[] bts=new byte[(int)tmpFile.length()];
						fileInputStream.read(bts);
						long updatetime=0;
						
						int l=bts.length/1024000;
						for (int j=0;j<bts.length/1024000;j++) {
							printStream.write(bts, j*1024000, 1024000);
							printStream.flush();
							hasSent+=1024000;
							if (System.currentTimeMillis()-updatetime>100)
							{
								publishProgress(0,i+1,tmpFile);
								updatetime=System.currentTimeMillis();
							}
							Log.w("发送数据包", j+"");
							if (isCancelled()) {
								fileInputStream.close();
								printStream.close();
								socket.close();
								String mm="文件发送取消，已发送"+FormetFileSize(hasSent);
								if (!isCn) mm="File transport canceled, "+FormetFileSize(hasSent)+" bytes sent";
								eventHandler.sendMessage(eventHandler.obtainMessage(0, 
										mm));
								return true;
							}
						}
						
						printStream.write(bts,l*1024000,bts.length-l*1024000);
						printStream.flush();
						hasSent+=(bts.length-l*1024000);
						publishProgress(0,i+1,tmpFile);
						if (isCancelled()) {
							fileInputStream.close();
							printStream.close();
							socket.close();
							String mm="文件发送取消，已发送"+FormetFileSize(hasSent);
							if (!isCn) mm="File transport canceled, "+FormetFileSize(hasSent)+" bytes sent";
							eventHandler.sendMessage(eventHandler.obtainMessage(0, 
									mm));
							return true;
						}
						fileInputStream.close();
						xx=new char[1000];
						bf.read(xx);
						msg=new String(xx).trim();
						if (!msg.equals("ok")) {
							socket.close();
							eventHandler.sendMessage(eventHandler.obtainMessage(0, resources.getString(R.string.details50)));
							return true;
						}
					}
					//printWriter.print("ok");
					//printWriter.flush();
					printStream.write("ok".getBytes());
					printStream.flush();
					eventHandler.sendMessage(eventHandler.obtainMessage(5));
				}
			} catch (IOException e) {
				eventHandler.sendMessage(eventHandler.obtainMessage(0,resources.getString(R.string.details25)+devicemsg.name));
				Log.w("send file error", e.getMessage());}
			finally {isfileing=false;}	
			return true;
		}
		
		protected void onProgressUpdate(Object... values) {
			long i=hasSent;
			int j=(Integer)values[1];
			String name=((File)values[2]).getName();
			String string;
			string="发送根目录："+tmpFile.getPath()+"\n\n正在发送第"+j+"个文件，共计"+fileNum+"个文件\n\n";
			string+="当前任务：\n"+name+" ("+FormetFileSize(((File)values[2]).length())+")\n\n";
			string+="已发送 "+i+"B/"+length+"B\n";
			string+="("+FormetFileSize(i)+"/"+FormetFileSize(length)+")";
			if (!isCn) {
				string="Root directory: "+tmpFile.getPath()+"\n\nSending files: "+j+"/"+fileNum;
				string+="\n\nCurrently sending: \n"+name+" ("+FormetFileSize(((File)values[2]).length())+")\n\n";
				string+=i+"B/"+length+"B\n";
				string+="("+FormetFileSize(i)+"/"+FormetFileSize(length)+")";
			}
			progressDialog.setMessage(string);
			progressDialog.setProgress((int)((i+0.0)/length*100));
		}
		
		protected void onPostExecute(Boolean b) {
			progressDialog.dismiss();
			isfileing=false;
		}
		
		protected void onCancelled() {
			try {
				socket.close();
			} catch (IOException e) {}
			progressDialog.dismiss();
			isfileing=false;
			super.onCancelled();
		}
	}
	
	double exittime=0;
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN) {
			if (looking) {
				if (System.currentTimeMillis()-exittime>2000) {
					Toast.makeText(this, R.string.details51, Toast.LENGTH_SHORT).show();
					exittime=System.currentTimeMillis();
				}
				else
				{
					exittime=0;
					sendString="";
					sendtype=0;
					looking=false;
					showDirectory();
				}
				return true;
			}
			if (islistening) {
				if (System.currentTimeMillis()-exittime>2000) {
					Toast.makeText(this, R.string.details52, Toast.LENGTH_SHORT).show();
					exittime=System.currentTimeMillis();
				}
				else
				{
					exittime=0;
					requestingTask.cancel(true);
					showDirectory();
				}
				return true;
			}
			else if (!file.equals(Environment.getExternalStorageDirectory())) {
				file=file.getParentFile();
				nowDirectory=file.getPath();
				showDirectory();
				return true;
			}
			else {
				if (System.currentTimeMillis()-exittime>2000) {
					Toast.makeText(this, R.string.details54, Toast.LENGTH_SHORT).show();
					exittime=System.currentTimeMillis();
				}
				else
				{
					exittime=0;
					finish();
				}
				return true;
			}
			
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private String getMIMEType(File file) {
		String[][] MIME_TABLE={
			{".3gp","video/3gpp"},{".apk","application/vnd.android.package-archive"},
			{".asf","video/x-ms-asf"},{".avi","video/x-msvideo"},
			{".bmp","image/bmp"},{".txt","text/plain"},{".jpg","image/jpeg"},
			{".jpeg","image/jpeg"},{".jpz","image/jpeg"},
			{".doc","application/msword"},{".docx","application/msword"},
			{".png","image/png"},
			{".html","text/html"},{".htm","text/html"},{".ico","image/x-icon"},
			{".js","application/x-javascript"},{".mp3","audio/x-mpeg"},
			{".mov","video/quicktime"},{".mpg","video/x-mpeg"},{".mp4","video/mp4"},
			{".pdf","application/pdf"},{".ppt","application/vnd.ms-powerpoint"},
			{".swf","application/x-shockwave-flash"},{".wav","audio/x-wav"},
			{".zip","application/zip"},{".rar","application/x-rar-compressed"},
			{".rm","audio/x-pn-realaudio"},{".rmvb","audio/x-pn-realaudio"}
		};
		String type="*/*";
		String end=getSuffix(file);
		if (end.equals("")) return type;
		for (int i=0;i<MIME_TABLE.length;i++) {
			if (end.equals(MIME_TABLE[i][0])) type=MIME_TABLE[i][1];
		}
		return type;
	}
	
	public String getSuffix(File file) {
		String fname=file.getName();
		int doIndex=fname.lastIndexOf(".");
		if (doIndex<0) return "";
		return fname.substring(doIndex, fname.length()).toLowerCase(Locale.getDefault());
	}
	
	private void getOverflowMenu() {
		 
	     try {
	        ViewConfiguration config = ViewConfiguration.get(this);
	        Field menuKeyField = ViewConfiguration.class
	        		.getDeclaredField("sHasPermanentMenuKey");
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	private void setIconEnable(Menu menu, boolean enable) {
		try {
			Class<?> clazz = Class.forName("com.android.internal.view.menu.MenuBuilder");
			Method m = clazz.getDeclaredMethod("setOptionalIconsVisible", boolean.class);
			m.setAccessible(true);
			m.invoke(menu, enable);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private byte[] toLH(double d) {
		long n=Double.doubleToLongBits(d);
		byte[] b=new byte[8];
		for (int i=0;i<=7;i++)
			b[i]=(byte)((n>>(8*i))&0xFF);
		return b;
	}
	
	private ArrayList<File> getArrayList(File file) {
		ArrayList<File> arrayList=new ArrayList<File>();
		if (file.isFile())
			arrayList.add(file);
		else if (file.isDirectory()) {
			File[] fls=file.listFiles();
			for (int j=0;j<fls.length;j++)
				arrayList.addAll(getArrayList(fls[j]));
		}
		return arrayList;
	}
	
	private String getRelativePath(File root,File file) {
		String x1=root.getParentFile().getPath(),x2=file.getPath();
		return x2.substring(x1.length()+1, x2.length());
	}
	
	private boolean phoneInUse() {
		TelephonyManager manager=(TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		return manager.getCallState()!=0;
	}
}
