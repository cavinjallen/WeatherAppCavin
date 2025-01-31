package com.dreamteam2.weatherapp

import androidx.compose.foundation.shape.CircleShape
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam2.weatherapp.ui.theme.WeatherAppTheme
import kotlin.math.roundToInt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.roundToLong

/*
MainActivity
-------------------------------------------------------------
The class creates an instance of the MainViewModel class and takes all the data that MainViewModel
pulls from the API and builds the UI of the application.

 */
class MainActivity : ComponentActivity() {
    val viewModel: MainViewModel = MainViewModel()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestNewLocationData()
        getLastLocation()
        setContent {
            WeatherAppTheme {
                mainLayout(viewModel)
            }
        }
    }
    val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        requestNewLocationData()
                        viewModel.lat.value = location.latitude
                        viewModel.long.value = location.longitude
                        runBlocking {
                            viewModel.fetchByString(viewModel.lat.toString() + ", " + viewModel.long.toString())
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    fun requestNewLocationData() {
        var mLocationRequest = LocationRequest.create()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Looper.myLooper()?.let {
            mFusedLocationClient!!.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                it
            )
        }
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
            viewModel.lat.value = mLastLocation.latitude
            viewModel.long.value = mLastLocation.longitude
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.N)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "CoroutineCreationDuringComposition")
@Composable
fun mainLayout(viewModel: MainViewModel){
    val navController = rememberNavController()
    //var change: Int = 0
    //Scaffolding helps keep the top bar always at the top of the screen


    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
//                                    MaterialTheme.colors.primaryVariant allows us to use
//                                    different color themes for light and dark mode
                    .background(MaterialTheme.colors.primaryVariant)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .height(IntrinsicSize.Min)

            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.Start)
                        .fillMaxWidth()
                ){
                    searchbar(viewModel)
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                ){
                    currLocationButton(viewModel)
                }

            }
        },
        bottomBar = {
            bottomNavBar(navController)

        },

    ){
        navigation(navController, viewModel, LocalContext.current)

    }
    LaunchedEffect(true){
        viewModel.fetchByString("Key West, Florida")
    }
}


@Composable
fun homeScreen(viewModel: MainViewModel){

    val loadStatus by viewModel.loadStatus.collectAsState()
    val status by viewModel.status.collectAsState()
    val forecastHourly by viewModel.forecastHourly.collectAsState()
    val foreCastPointEndpointTemperature by viewModel.forecast.collectAsState()
    val gridpointProperties by viewModel.gridpointsProperties.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp)
        .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(15.dp))
        if (status == null && loadStatus == MainViewModel.LoadStatus.error) {
            Text(
                text = "Connection Error...",
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                color = MaterialTheme.colors.primary
            )
        } else if (loadStatus == MainViewModel.LoadStatus.attempt){
            Text(
                text = "Loading...",
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                color = MaterialTheme.colors.primary
            )
        } else if (loadStatus == MainViewModel.LoadStatus.error){
            Text(
                text = "Please Choose Location...",
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                color = MaterialTheme.colors.primary
            )
        } else if (loadStatus == MainViewModel.LoadStatus.success) {
            if(gridpointProperties?.properties?.apparentTemperature?.values?.isNotEmpty() == true || forecastHourly?.propertiesInForecast?.period?.isNotEmpty() == true){
                today(viewModel)
            }else{
                Text(text ="Today view has been excluded given current temperature and other data has not been published for the requested location at this time", color = MaterialTheme.colors.primary)
            }
            Spacer(modifier = Modifier.height(15.dp))
            if(forecastHourly?.propertiesInForecast?.period?.isNotEmpty() == true){
                hourly(viewModel)
            }else{
                Text(text ="Hourly view has been excluded given forecast data has not been published for the requested location at this time", color = MaterialTheme.colors.primary)
            }
            Spacer(modifier = Modifier.height(15.dp))
            if (foreCastPointEndpointTemperature?.propertiesInForecast?.period?.isNotEmpty() == true){
                dailyForecast(viewModel)
            }else{
                Text(text ="Daily view has been excluded given forecast data has not been published for the requested location at this time", color = MaterialTheme.colors.primary)
            }
            Spacer(modifier = Modifier.height(15.dp))
            if(gridpointProperties?.properties?.apparentTemperature?.values?.isNotEmpty() == true){
                bottom(viewModel)
            }else{
                Text(text ="Data view has been excluded given data has not been published for the requested location at this time", color = MaterialTheme.colors.primary)
            }
            Spacer(modifier = Modifier.height(15.dp))
            saveLocation(viewModel)
            Spacer(modifier = Modifier.height(15.dp))
            temperatureButton(viewModel)
            Spacer(modifier = Modifier.height(50.dp))
        } else {
            Text(
                text = "Invalid Location...",
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                color = MaterialTheme.colors.primary
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
    }
}

@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun locs(viewModel: MainViewModel, context: Context, navController: NavController){
    val coordinates by viewModel.coordinates.collectAsState()
    /*
    var firsPer = com.dreamteam2.weatherapp.Location("name")
    var list: ArrayList<com.dreamteam2.weatherapp.Location>? = ArrayList<com.dreamteam2.weatherapp.Location>()
    list?.add(firsPer)
     */
    var files: Array<String> = context.fileList()
    if(!files.contains("savedLocs.txt")){
        saveToInternalStorage(context, "")
    }
    var locations: String = readFromInternalStorage(context)
    var locArray: ArrayList<String> = ArrayList()
    if (locations != ""){
        locArray = locations.split("\n") as ArrayList<String>
        locArray.removeIf{ it == "" }
    }



    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)
        .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally) {
                for (str:String in locArray){
                    locButton(viewModel, str, navController, context)
                }
        Spacer(modifier = Modifier.height(50.dp))
            }

}

@Composable
fun locButton(viewModel: MainViewModel, name: String, navController: NavController, context: Context){
    val coordinates by viewModel.coordinates.collectAsState()
    Button(modifier = Modifier
        .fillMaxSize()
        .height(IntrinsicSize.Max)
        .padding(15.dp),
        onClick = {
            runBlocking {
                viewModel.fetchByString(name)
            }
            navController.navigate("home")
        },
        //border = BorderStroke(4.dp, MaterialTheme.colors.primaryVariant),
        shape = RoundedCornerShape(10),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primaryVariant,
            contentColor = Color.Black)
        /*
colors = ButtonDefaults.buttonColors(
    backgroundColor = MaterialTheme.colors.primaryVariant,
    contentColor = Color.Black)

         */
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(textAlign = TextAlign.Center, text = name, fontSize = 30.sp, modifier = Modifier.padding(15.dp))
            Button(
                onClick = {
                    var saveStr:String = readFromInternalStorage(context)
                    saveStr = saveStr.replace(name, "")
                    saveToInternalStorage(context, saveStr)
                    navController.navigate("home")
                },
                elevation =  ButtonDefaults.elevation(
                    defaultElevation = 10.dp,
                    pressedElevation = 15.dp,
                    disabledElevation = 0.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp,Color.Black),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primaryVariant,
                    contentColor = Color.Black)) {
                Text(textAlign = TextAlign.Center, text = "Delete", fontSize = 30.sp, modifier = Modifier.padding(10.dp))
            }
        }
    }
}



@Composable
fun currLocationButton(viewModel : MainViewModel){
    val lat by viewModel.lat.collectAsState()
    val long by viewModel.long.collectAsState()
    IconButton(
        onClick = {
            runBlocking {
                viewModel.fetchByString(lat.toString() + ", " +  long.toString())
            }
        }
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = "",
            modifier = Modifier
                .padding(15.dp)
                .size(24.dp),
        )
    }
}

/*
* fun City
* @param MainViewModel
* Displays location name and coordinates on UI.
*
* Design pattern implementation - State
 */
@Composable
fun city(viewModel: MainViewModel){
    val lat by viewModel.lat.collectAsState()
    val long by viewModel.long.collectAsState()

    Text(
        text = lat.toString() + " " + long.toString(),
        textAlign = TextAlign.Center,
        fontSize = 30.sp
    )
}

@Composable
fun searchbar(viewModel: MainViewModel){
    val lat by viewModel.lat.collectAsState()
    val long by viewModel.long.collectAsState()
    val gridPointEndpoints by viewModel.gridPointEndpoints.collectAsState()
    val searchTextState = remember { mutableStateOf(TextFieldValue("")) }
    Row(
        modifier = Modifier
            .width(350.dp)
            .height(IntrinsicSize.Min)
    ) {
        val focusManager = LocalFocusManager.current
        TextField(
            value = searchTextState.value,
            onValueChange = { value ->
                searchTextState.value = value
            },
            placeholder = {
                Text(
                    text = gridPointEndpoints?.properties?.relativeLocation?.properties?.city + ", " + gridPointEndpoints?.properties?.relativeLocation?.properties?.state,
                    color = Color.Black
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "",
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(24.dp)
                )
            },
            trailingIcon = {
                if (searchTextState.value != TextFieldValue("")) {
                    IconButton(
                        onClick = {
                            searchTextState.value =
                                TextFieldValue("") // Remove text from TextField when you press the 'X' icon
                        }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "",
                            modifier = Modifier
                                .padding(15.dp)
                                .size(24.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color.Black,
                cursorColor = Color.White,
                leadingIconColor = Color.Black,
                trailingIconColor = Color.Black,
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    viewModel.viewModelScope.launch {
                        viewModel.fetchByString(searchString = searchTextState.value.text)
                    }
                    focusManager.clearFocus()
                    searchTextState.value = TextFieldValue("")
                },
            )
        )
    }
}


/*
* fun today
* This function displays data relevant for "today" which includes the current
* temperature, a description of the weather, a feels like temperature, a wind speed and
* direction, and a high-low temperature for the day. Along with styling elements.
* @param MainViewModel
*
* Design pattern implementation - State
 */
@Composable
fun today(viewModel: MainViewModel){
    val forecastHourly by viewModel.forecastHourly.collectAsState()
    val gridpointProperties by viewModel.gridpointsProperties.collectAsState()
    val celsius by viewModel.isCelsius.collectAsState()
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.primaryVariant, shape = RoundedCornerShape(25.dp))
            .fillMaxSize()
    ){
        Row(
            modifier = Modifier
                .padding(20.dp)
                .height(IntrinsicSize.Max)
        ){
            if (forecastHourly?.propertiesInForecast?.period?.isNotEmpty() == true){
                Column(
                    modifier = Modifier
                        .width(130.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    var tempString: String = ""
                    var temperature: Double? = forecastHourly?.propertiesInForecast?.period?.get(0)?.temperature?.toDouble()
                    if(celsius == true){
                        if (temperature != null) {
                            tempString = ((temperature -32)*(0.55555)).roundToInt().toString()
                        }
                    }
                    else{
                        tempString = temperature?.roundToInt().toString()
                    }
                    Text(
                        text = tempString + "°",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                        fontSize = 70.sp,
                    )
                }
            }
            Column{
                if(forecastHourly?.propertiesInForecast?.period?.isNotEmpty() == true) {
                    Row {
                        Text(
                            text = forecastHourly?.propertiesInForecast?.period?.get(0)?.shortForecast.toString(),
                            modifier = Modifier,
                            fontSize = 25.sp,
                        )
                    }
                }
                if(gridpointProperties?.properties?.apparentTemperature?.values?.isNotEmpty() == true){
                    Row {
                        var aTemp: Long? =
                            gridpointProperties?.properties?.apparentTemperature?.values?.get(0)?.value?.roundToLong()
                        var aTempString: String
                        var sign: String = ""
                        if (aTemp == null) {
                            aTempString = "Loading"
                        } else {
                            if (celsius == false) {
                                aTempString = (aTemp * 1.8 + 32).roundToInt().toString()
                                sign = "F"
                            }
                            else{
                                aTempString = aTemp.toString()
                                sign = "C"
                            }
                        }

                        Text(
                            text = "Feels Like: " + aTempString + "°" + sign,
                            textAlign = TextAlign.Center,
                            modifier = Modifier,
                            fontSize = 20.sp
                        )
                    }
                    Row{
                        var aTemp: Double? =
                            gridpointProperties?.properties?.apparentTemperature?.values?.get(0)?.value
                        var windS: Double? = gridpointProperties?.properties?.windSpeed?.values?.get(0)?.value
                        var windSString: String
                        if (windS == null){
                            windSString = "Loading"
                        }else{
                            windSString = ((windS * 0.621371192).roundToInt()).toString()
                        }
                        var windD: Double? = gridpointProperties?.properties?.windDirection?.values?.get(0)?.value
                        var windDString: String
                        if (aTemp == null) {
                            windDString = "Loading"
                        }else if (22.0 < windD!! && windD!! <  68.0){
                            windDString = "NE ⬋"
                        }else if (67.0 < windD!! && windD!! <  113.0){
                            windDString = "E ⬅"
                        }else if (112.0 < windD!! && windD!! <  158.0){
                            windDString = "SE ⬉"
                        }else if (157.0 < windD!! && windD!! <  203.0){
                            windDString = "S ⬆"
                        }else if (202.0 < windD!! && windD!! <  248.0){
                            windDString = "SW ⬈"
                        }else if (247.0 < windD!! && windD!! <  293.0){
                            windDString = "W ⮕"
                        }else if (292.0 < windD!! && windD!! <  338.0){
                            windDString = "NW ⬊"
                        }else{
                            windDString = "N ⬇"
                        }
                        Text(
                            text = "Wind: " + windSString + " mph " + windDString,
                            textAlign = TextAlign.Center,
                            modifier = Modifier,
                            fontSize = 20.sp
                        )
                    }
                    Row {
                        var minTemp: Double? =
                            gridpointProperties?.properties?.minTemperature?.values?.get(0)?.value
                        var minTempString: String
                        if (minTemp == null) {
                            minTempString = "Loading"
                        } else {
                            if(celsius == false) {
                                minTemp = (minTemp * 1.8) + 32
                                minTempString = minTemp.roundToInt().toString()
                            }
                            else{
                                minTempString = minTemp.roundToInt().toString()
                            }
                        }
                        var maxTemp: Double? =
                            gridpointProperties?.properties?.maxTemperature?.values?.get(0)?.value
                        var maxTempString: String
                        if (maxTemp == null) {
                            maxTempString = "Loading"
                        } else {
                            if(celsius == false) {
                                maxTempString = (maxTemp * 1.8 + 32).roundToInt().toString()
                            }
                            else{
                                maxTempString = maxTemp.roundToInt().toString()
                            }
                        }
                        Text(
                            text = "H: " + maxTempString + "°    " + "L: " + minTempString + "°",
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/*
 * fun hourly
 *
 * This function displays the hourly data for the specified location.
 * It displays the information in a 12 hour format with horizontal scroll and
 * uses degrees Fahrenheit for units.
 * The information is organized in a Jetpack Compose Row
 *
 * @param MainViewModel
 *
 * Design pattern implementation - State
 */
@Composable
fun hourly(viewModel: MainViewModel){
    val forecastHourly by viewModel.forecastHourly.collectAsState()
    val celsius by viewModel.isCelsius.collectAsState()
    Column( modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colors.primaryVariant, shape = RoundedCornerShape(25.dp))
        .horizontalScroll(rememberScrollState())
        .alpha(1.0F)
    ) {
        Row(
            modifier = Modifier.padding(20.dp)
        ) {
            forecastHourly?.let {
                if(it.propertiesInForecast?.period?.isNotEmpty() == true){
                    if(celsius == false){
                        for (i in 0..13) {
                            var allTimeStuff: String = it.propertiesInForecast?.period?.get(i)?.startTime.toString()
                            var ending = "AM"
                            allTimeStuff = allTimeStuff.substring(11, 13)
                            if(allTimeStuff.toInt() > 11){
                                ending = "PM"
                            }
                            if(allTimeStuff.toInt() % 12 == 0){
                                allTimeStuff = "12"
                            }
                            else
                            {
                                allTimeStuff = (allTimeStuff.toInt() % 12).toString()
                            }
                            if(i == 0){
                                ending = ""
                                allTimeStuff = "Now"
                            }
                            Column() {
                                Row(modifier = Modifier.padding(7.dp)) {
                                    Text(text = "$allTimeStuff",
                                        textAlign = TextAlign.Center,
                                        fontSize = 24.sp
                                        //fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "$ending",
                                        //textAlign = TextAlign.Center,
                                        fontSize = 15.sp,
                                        modifier = Modifier.offset(0.dp, 9.dp)
                                    )
                                }
                                Row(modifier = Modifier.padding(0.dp)) {
                                    Text(text = it.propertiesInForecast?.period?.get(i)?.temperature.toString() + "°F   ",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.offset(5.dp, 0.dp),
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }
                    } else {
                        for (i in 0..13) {
                            var allTimeStuff: String = it.propertiesInForecast?.period?.get(i)?.startTime.toString()
                            var ending = "AM"
                            allTimeStuff = allTimeStuff.substring(11, 13)
                            if(allTimeStuff.toInt() > 11){
                                ending = "PM"
                            }
                            if(allTimeStuff.toInt() % 12 == 0){
                                allTimeStuff = "12"
                            }
                            else
                            {
                                allTimeStuff = (allTimeStuff.toInt() % 12).toString()
                            }
                            if(i == 0){
                                ending = ""
                                allTimeStuff = "Now"
                            }
                            Column() {
                                Row(modifier = Modifier.padding(7.dp)) {
                                    Text(text = "$allTimeStuff",
                                        textAlign = TextAlign.Center,
                                        fontSize = 24.sp
                                        //fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "$ending",
                                        //textAlign = TextAlign.Center,
                                        fontSize = 15.sp,
                                        modifier = Modifier.offset(0.dp, 9.dp)
                                    )
                                }
                                Row(modifier = Modifier.padding(0.dp)) {
                                    var temperature2 = it.propertiesInForecast?.period?.get(i)?.temperature?.toDouble()
                                    if (temperature2 != null) {
                                        temperature2 = ((temperature2 - 32) * (0.5555))
                                    }
                                    Text(text = temperature2?.roundToInt().toString() + "°C   ",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.offset(5.dp, 0.dp),
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: Text(text = "Loading...")
        }
    }
}

/*
 * fun dailyForecast
 *
 * This function displays the daily data for the specified location.
 * It displays the information for the morning and evening of each day for 7 days.
 * It displays the information in a Jetpack Compose Column
 * All units are in degrees Fahrenheit
 *
 * @param MainViewModel
 *
 * Design pattern implementation - State
 */
@Composable
fun dailyForecast(viewModel: MainViewModel) {
    val foreCastPointEndpointTemperature by viewModel.forecast.collectAsState()
    val celsius by viewModel.isCelsius.collectAsState()
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.primaryVariant, shape = RoundedCornerShape(25.dp))
    ){
        Column(
            modifier = Modifier
                .padding(20.dp)
        ) {
            foreCastPointEndpointTemperature?.let {
                for (i in 0..13) {
                    var temperature = it.propertiesInForecast?.period?.get(i)?.temperature
                    if(celsius == false){
                        dataRow(it.propertiesInForecast?.period?.get(i)?.name.toString(), temperature.toString() + "°F")
                }
                    else{
                        if (temperature != null) {
                            temperature = ((temperature - 32) * (0.55555)).toInt()
                        }
                        dataRow(it.propertiesInForecast?.period?.get(i)?.name.toString(), temperature.toString() + "°C")
                    }
                    }
            } ?: Text(text = "Loading...")
        }
    }
}
/*
* fun bottom
* @param MainViewModel
* Grabs the Dewpoint, Cloud Coverage, and Humididity data points from gridpointProperties and
* passes the to the function dataRow to display them at the bottom of the screen.
*
* Design pattern implementation - State
 */
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun bottom(viewModel: MainViewModel){
    val gridpointProperties by viewModel.gridpointsProperties.collectAsState()
    val celsius by viewModel.isCelsius.collectAsState()
    Column( modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colors.primaryVariant, shape = RoundedCornerShape(25.dp))
        .alpha(1.0F)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            dataRow("Humidity", gridpointProperties?.properties?.relativeHumidity?.values?.get(0)?.value.toString() + " %")
            var dewpoint: Long? =
                gridpointProperties?.properties?.dewpoint?.values?.get(0)?.value?.roundToLong()
            var dewpointString: String
            if (dewpoint == null) {
                dewpointString = "Loading"
            } else {
                if(celsius == true) {
                    dewpointString = dewpoint.toString()
                }
                else{
                    dewpointString = ((dewpoint * 1.8) + 32).toString()

                }
            }
            if(celsius == true) {
                dataRow("Dewpoint", dewpointString + " °C")
            }
            else{
                dataRow("Dewpoint", dewpointString + " °F")
            }
            dataRow("Cloud Coverage", gridpointProperties?.properties?.skyCover?.values?.get(0)?.value.toString() + " %")
            if (!(gridpointProperties?.properties?.potentialOf50mphWindGusts?.values.isNullOrEmpty())) {
                dataRow("Wind Advisory", gridpointProperties?.properties?.potentialOf50mphWindGusts?.values.toString())
            }
        }
    }
}

@Composable
fun saveLocation(viewModel: MainViewModel){
    val coordinates by viewModel.coordinates.collectAsState()
    val context = LocalContext.current
    Column( modifier = Modifier
        .fillMaxWidth()
        .alpha(1.0F)
    ) {
        Button(modifier = Modifier
            .fillMaxSize()
            .height(IntrinsicSize.Max)
            .padding(15.dp),
            onClick = {

                var files: Array<String> = context.fileList()
                if(!files.contains("savedLocs.txt")){
                    saveToInternalStorage(context, "")
                }

                var saveStr:String = readFromInternalStorage(context)
                try {
                    if (!saveStr.contains(coordinates?.get(0)?.displayName.toString())) {
                        saveStr = saveStr + "\n" + coordinates?.get(0)?.displayName.toString()
                    }
                }
                catch (e: java.lang.IndexOutOfBoundsException){

                }

                saveToInternalStorage(context, saveStr)
            },
            //border = BorderStroke(4.dp, MaterialTheme.colors.primaryVariant),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primaryVariant,
                contentColor = Color.Black
            )
        ) {
            Text(textAlign = TextAlign.Center, text = "Save Location", fontSize = 20.sp)
        }
    }
}


fun saveToInternalStorage(context: Context, msg: String) {

        val fos: FileOutputStream =
            context.openFileOutput("savedLocs.txt", Context.MODE_PRIVATE)
        fos.write(msg.toByteArray())
        fos.flush()
        fos.close()
     }


fun readFromInternalStorage(context: Context): String {

    val fin: FileInputStream = context.openFileInput("savedLocs.txt")
    var a: Int
    val temp = StringBuilder()
    //while (fin.read().also { a = it } != -1) {
    //    temp.append(a.toChar())
    //}
    var i = 0
    while (i < fin.available().toInt()){
        temp.append(fin.read().toChar())
    }
    fin.close()

    return temp.toString()
}

/*
* fun dataRow
* @param String, String
* Styling elements to display the Dewpoint, Cloud Coverage, and Humidity
 */
@Composable
fun dataRow(leftText: String, rightText: String,){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.Start)
                .height(27.dp)
        ){
            Text(
                text = leftText,
                fontSize = 19.sp
            )
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.End)
                .height(27.dp)
        ){
            Text(
                text = rightText,
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun bottomNavBar(navController: NavController) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Locations,
    )
    BottomNavigation(
        backgroundColor = MaterialTheme.colors.primaryVariant,
        contentColor = Color.White,

        ){
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            BottomNavigationItem(
                icon = {
                    Icon(
                        painterResource(id = item.icon),
                        contentDescription = item.title,
                        Modifier.size(30.dp)
                    )
                },
                label = { Text(text = item.title) },
                selectedContentColor = Color.White,
                unselectedContentColor = Color.Black.copy(0.4f),
                alwaysShowLabel = true,
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
                //modifier = Modifier.size(5.dp, 5.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun navigation(navController: NavHostController, viewModel: MainViewModel, context: Context) {
    //val viewModel: MainViewModel = MainViewModel()
    NavHost(navController, startDestination = NavigationItem.Home.route) {
        composable(NavigationItem.Home.route) {
            homeScreen(viewModel)
        }
        composable(NavigationItem.Locations.route) {
            locs(viewModel, context, navController)
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun temperatureButton(viewModel: MainViewModel) {
    val celsius by viewModel.isCelsius.collectAsState()
    Button(
        onClick = { viewModel.isCelsius.value = !viewModel.isCelsius.value!! },
        elevation =  ButtonDefaults.elevation(
            defaultElevation = 10.dp,
            pressedElevation = 15.dp,
            disabledElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primaryVariant,
            contentColor = Color.Black
        ),
        modifier = Modifier.height(40.dp).width(130.dp)){
        if(celsius == false) {
            Text(text = "Celsius")
        }
        else{
            Text(text = "Fahrenheit")
        }
    }
}
