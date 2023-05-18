package com.example.carspy

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.carspy.databinding.ActivityMainBinding
import okhttp3.*
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var speedBtn = findViewById<Button>(R.id.startBtn)
        speedBtn.setOnClickListener {
            loadWebView()
            speedBtn.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadWebView() {
        webView = findViewById<WebView>(R.id.webView)

        var brands = "https://www.avito.ru/catalog/auto"
//        var car1 = "https://www.avito.ru/catalog/auto/audi/100/c4/sedan/specs-ASgBAgICBUTgtg3elyjitg3gmSjmtg3Ktyjqtg3GginQvA78m9EB"
        webView.loadUrl(brands)
        // Enable JavaScript in the WebView
        webView.settings.javaScriptEnabled = true
        // Get the WebSettings for the WebView
        webView.settings.safeBrowsingEnabled = false
        webView.settings.domStorageEnabled = true

        //Add our a bridge between iframe's js and our code
        webView.addJavascriptInterface(WebInterface(this), "Bridge")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String) {
                super.onPageFinished(view, url)
                if (view != null) injectJavaScript(view)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                println("------------------------------onConsoleMessage" + consoleMessage.message())
                return true
            }

        }

    }

    private fun injectJavaScript(view: WebView) {
        view.loadUrl(
            """
            javascript:(function(){
            
              function sendToSlack(data){
                 console.log('Calling sendToSlack');
                var raw = JSON.stringify({
                  "text": data
                });
              
                Bridge.sendDataToServer(raw);
                
            }
            
            
            
    function extractSpecs(element) {
        let obj = {};
        console.log('There are ---------- '+element.children.length);
        for (let i = 0; i < element.children.length; i++) {
            const current = element.children[i];
            const property = current.children[0].innerHTML;
            const details = current.children[1];
            obj[property] = {};
            for (let x = 0; x < details.children.length; x++) {
                const row = details.children[x];
                for (let y = 0; y < row.children.length; y++) {
                    const data = row.children[y];
                    const key = data.innerHTML.split(':')[0].replace(/&nbsp;/g, '');
                    const value = data.children[0].innerHTML.replace(/&nbsp;/g, '');
                    obj[property][key] = value;
                }
            }
        }
        console.log("Brands are: "+JSON.stringify(obj));
        return obj;
    }

    function scrapeCarDetails() {
        var element = document.querySelector('[data-marker="bottom-sheet/content"]');
        var specs = element.children[1].children[4].firstChild;
        var json = extractSpecs(specs.children[1]);
        const obj = JSON.stringify({
            "title": element.children[1].children[2].firstChild.innerHTML,
            "specs": json
        });
        Bridge.sendDataToServer(JSON.stringify({
            "text":  obj
        }));
        return true;
    }
    
   var cars = {
            "token": "",
            "brands":[]
   };

   function loopThroughChildren(element){
        if(element.children.length === 0 || typeof element.children === 'undefined') return element.innerHTML;
        return loopThroughChildren(element.firstChild);
   }
   function scrapeBody(){
    
    Bridge.sendDataToServer(JSON.stringify(cars));
    return;
    let indexCar = localStorage.getItem('indexCar') != null ? parseInt(localStorage.getItem('indexCar')) : 0;
    let indexModel = localStorage.getItem('indexModel') != null ? parseInt(localStorage.getItem('indexModel')) : 0;
    
    
    if(typeof cars.brands[indexCar] === 'undefined'){
            console.log('------------------Finished processing carModels');
            return;
    }
    
    if(indexCar === 43){
     localStorage.setItem('indexCar',indexCar+1);
     scrapeBody();
     return;
    }
    console.log("    Car: "+indexCar+"  Model: "+indexModel);
    const currentCar = cars.brands[indexCar];
    const currentModel = currentCar.models[indexModel];
    
    if(indexModel >= currentCar.models.length - 1){
            if(indexCar == 0 && typeof cars.brands[indexCar].models[indexModel] === 'undefined'){
                Bridge.openBridge();
                sendToSlack(JSON.stringify(currentCar));
            }
            console.log('---------Moving to next Model '+indexCar+'  '+indexModel);
            localStorage.setItem('indexCar',indexCar+1);
            localStorage.setItem('indexModel',0);
            scrapeBody();
            return;
    }else{
        console.log('----------------------indexModel: '+indexModel);
    }
    const carLink = currentCar.modelLink.split("-")[0];
    
    if(window.location.href !== 'https://m.avito.ru/catalog/auto/'+carLink+'/'+currentModel.yearLink){
        window.location.href = 'https://m.avito.ru/catalog/auto/'+carLink+'/'+currentModel.yearLink;
        return;
    }
   
       
        const title = document.querySelector('[data-marker="model-card/title"]');
        console.log('***********'+title.innerHTML);
        const parent = title.nextElementSibling;
        let year =  0;
        let bodies = 0;
        let allBodies = [];
        const all = parent.firstChild.children;

        
        currentCar['models'][indexModel]['years'] = {};
        
        for(let i = 0; i<all.length;i++){
                year =  all[i].firstChild.firstChild.innerHTML;
                bodies = all[i].firstChild.children[2].children;
                allBodies = [];
                if(!bodies) continue;
                
                    for(let i=0;i<bodies.length;i++){
                        if(allBodies.indexOf(bodies[0].lastChild.lastChild.lastChild.innerHTML) < 0){
                            allBodies.push(bodies[0].lastChild.lastChild.lastChild.innerHTML);
                        }
                    }
                currentCar['models'][indexModel]['years'][year] = allBodies;
        }
             
        
    
           
             saveToStorage(); 
    
    
    setTimeout(()=>{
        localStorage.setItem('indexModel',indexModel+1);
        scrapeBody();
    },100);
   
   }

    function scrapeModels(started = false){
        let index = parseInt(localStorage.getItem('index')) ?? 0;
       
        if(started){
             cars = JSON.parse(localStorage.getItem('allBrands'));
             if(index >= cars.brands.length){
                    
                    scrapeBody();
                   
                    return;
            } 
            const modelsURL = 'https://m.avito.ru/catalog/auto/'+cars['brands'][index]['modelLink'];
            if(!cars['brands'][index]['modelLink']){
                        saveToStorage();
                        localStorage.setItem('index',index+1);
                        Bridge.openBridge();
                        sendToSlack(JSON.stringify(cars['brands'][index]));
                        scrapeModels(true);
                        return;
            }
        if(window.location.href !== modelsURL){
            window.location.href = modelsURL;
        }
            setTimeout(()=>{
                var title = document.querySelector('[data-marker="model-card/title"]'); 
                var all = title.nextElementSibling.children;
                if(!all.length){
                    window.location.reload();
                    scrapeModels(true);
                    return;
                }
                         for(let x = 0;x<all.length;x++){
                                    extractURL(all[x].firstChild.href);
                                   cars['brands'][index]['models'].push({
                                        "name":  loopThroughChildren(all[x]),
                                        "yearLink": extractURL(all[x].firstChild.href),
                                    });
                         }
                        saveToStorage();
                        localStorage.setItem('index',index+1);
                        Bridge.openBridge();
                        scrapeModels(true);
                },500);
            return;
        }
         console.log('Running scrapeModels');
         const modelsURL = 'https://m.avito.ru/catalog/auto/'+cars['brands'][index]['modelLink'];
         saveToStorage();
         window.location.href = modelsURL;
   }
   function extractURL(url){
          if(!url) return "";
          const parts = url.split("/");
          return parts[parts.length - 1];
   }

    function scrapeBrands(){


      if(savedAlready()){
           cars = JSON.parse(localStorage.getItem('allBrands'));
           scrapeModels(true);
           return;
      }

  
        var start = document.querySelector('[data-marker="model-card/title"]');
        var temp = start.nextElementSibling;
        var brandContianer = temp.nextElementSibling;
        brandContianer.lastChild.firstChild.click();
        var all = document.querySelectorAll('[data-marker="rubricator/row"]');
        if(!all.length){
            window.location.reload();
            scrapeBrands();
            return;
        }
        for(let i = 0;i<all.length-1;i++){
           cars['brands'].push(
            {
                "name": loopThroughChildren(all[i]),
                "modelLink": extractURL(all[i].firstChild.href),
                "models":[]
            }
           )  
        }
 
        saveToStorage();
        localStorage.setItem('index',0);
        scrapeModels();
    }
    function removeAll(){
        localStorage.removeItem('allBrands');
        localStorage.removeItem('index');
    }
    function saveToStorage(){
            localStorage.setItem('allBrands',JSON.stringify(cars));
    }
    function savedAlready(){
        return localStorage.getItem('allBrands') !== null;
    }
    function start(){
        if(!cars.brands.length) scrapeBrands();
    }
   
    setTimeout(()=>{
       console.log(window.location.href);
        scrapeBrands();
    },300);
   
  
   
    
    
    })()
        """.trimIndent()
        )
    }

    class WebInterface(private val mContext: Context) {
        var sentDataToServer = false
        private fun showToast(message: String) {
            Handler(Looper.getMainLooper()).post {
                val toast = Toast.makeText(mContext, message, Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        private fun toLocalHost(data: String) {
            val client = OkHttpClient().newBuilder()
                .build()
            val mediaType = MediaType.parse("application/json")
            val body = RequestBody.create(mediaType, data)
            val request: Request = Request.Builder()
                .url("http://10.0.2.2/spy/log.php")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build()
            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    println("Request was successful!")
                } else {
                    println("Request failed!")
                }

                response.close()
            } catch (e: IOException) {
                println("An error occurred: ${e.message}")
            }


        }

        private fun writeToFile(data: String) {

        }

        private fun writeToBin(data: String) {
            print("Writing to bin----------------*****************************************************************");
            val client = OkHttpClient().newBuilder()
                .build()
            val mediaType = MediaType.parse("application/json")
            val body = RequestBody.create(mediaType, data)
            val request: Request = Request.Builder()
                .url("https://api.jsonbin.io/v3/b")
                .method("POST", body)
                .addHeader(
                    "X-Master-Key",
                    "$2b$10$2Gy97rJjPLykZFKAAgZoB.4s6B4GbUxHSfxuh.T1CAtN1pjHZv3ia"
                )
                .addHeader("Content-Type", "application/json")
                .build()
            val response = client.newCall(request).execute()
        }

        private fun sendToSlack(data: String) {
            print("*************** Sending Data To Slack")
            val client = OkHttpClient().newBuilder()
                .build()
            val mediaType = MediaType.parse("application/json")
            val body = RequestBody.create(mediaType, data)
            val request: Request = Request.Builder()
                .url("https://hooks.slack.com/services/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")
                .method("POST", body)
                .addHeader("Content-type", "application/json")
                .build()
            val response = client.newCall(request).execute()
        }

        @JavascriptInterface
        fun openBridge() {
            sentDataToServer = false
        }

        @JavascriptInterface
        fun sendDataToServer(data: String) {
            print("---------------Calling---------------sendDataToServer-----")
            if (!sentDataToServer) {
                toLocalHost(data)
                sentDataToServer = true
//                if(toSlack === true){
//                    sendToSlack(data);
//                }else{
//
//                }
            }

        }
    }
}