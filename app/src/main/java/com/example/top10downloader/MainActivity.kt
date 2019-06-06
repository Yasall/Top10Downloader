package com.example.top10downloader

import android.content.Context
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL
import kotlin.properties.Delegates

class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String = ""

    override fun toString(): String {
        return """
            name = $name
            artist = $artist
            releaseDate = $releaseDate
            imagerURL = $imageURL
        """.trimIndent()
    }
}

private const val STATE_FEEDURL = "feedurl"
private const val STATE_FEEDLIMIT = "feedlimit"

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    
    private var feedUrl: String =
        "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private var feedLimit = 10

    private var feedCached = "Invalidated"

    private var downloadData: DownloadData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState != null) {
            feedUrl = savedInstanceState.getString(STATE_FEEDURL)
            feedLimit = savedInstanceState.getInt(STATE_FEEDLIMIT)
        }
        downloadUrl(feedUrl.format(feedLimit))

    }

    private fun downloadUrl(feedUrl: String) {
        if (feedUrl != feedCached) {
            downloadData = DownloadData(this, xmlListView)
            downloadData?.execute(feedUrl)
            feedCached = feedUrl
        }

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)
        if (feedLimit == 10) {
            menu?.findItem(R.id.mnu10)?.isChecked = true
        } else {
            menu?.findItem(R.id.mnu25)?.isChecked = true
        }


        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //feed the URL to downloadUrl function

        when (item.itemId) {    //kotlin gna ignore if item is null if ? is used
            R.id.mnuFree ->
                feedUrl =
                    "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
            R.id.mnuPaid ->
                feedUrl =
                    "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"
            R.id.mnuSongs ->
                feedUrl =
                    "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
            R.id.mnuRefresh -> {
                feedCached = "Invalidated"
            }
            R.id.mnu10, R.id.mnu25 -> {
                if (!item.isChecked) {
                    item.isChecked = true
                    feedLimit = 35 - feedLimit
                }
            }
            else ->
                return super.onOptionsItemSelected(item)
        }

        downloadUrl(feedUrl.format(feedLimit))
        return true
    }


    override fun onDestroy() {
        super.onDestroy()
        downloadData?.cancel(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_FEEDURL, feedUrl)
        outState.putInt(STATE_FEEDLIMIT, feedLimit)
    }


    companion object {
        private class DownloadData(context: Context, listView: ListView) : AsyncTask<String, Void, String>() {
            private val TAG = "DownloadData"

            //prop not best to use for naming convention
            var propContext: Context by Delegates.notNull()
            var propListView: ListView  by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }

            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                val parseApplications = ParseApplications()
                parseApplications.parse(result)


                val entryAdapter = EntryAdapter(propContext, R.layout.list_record, parseApplications.applications)
                propListView.adapter = entryAdapter
            }

            override fun doInBackground(vararg url: String?): String {
                Log.d(TAG, "doInBackground: starts with ${url[0]}")
                val rssFeed = downloadXML(url[0])
                if (rssFeed.isEmpty()) {
                    Log.e(TAG, "doInBackGround:Error Downloading")
                }
                return rssFeed
            }


            private fun downloadXML(urlPath: String?): String {
                return URL(urlPath).readText()
            }
        }


    }
}
