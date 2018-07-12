const SQL = require('mssql')

let pool;

const config = {
    user:'sa',
    password: '',
    server: '140.127.196.75',
    port: 7777,
    database: 'HRV_DATA'
};

async function ConnectSQL(){
    try
    {
        pool = await SQL.connect(config)
        if (pool != null){
            console.log( 'Connected to SQL' );  
            console.log( '======================' );
        }
        else {
            console.log("SQL connected fail !")
            console.log( '======================' );
        }
    }
    catch (err)
    {
        console.log(err)
    }
}

async function SQLInsertData(command){
    try
    {
        if (pool != null){
            let result = await pool.request().query(command)
        }
        else {
            console.log("SQL disconnect !")
            console.log( '======================' );
        }
    }
    catch (err)
    {
        console.log(err)
    }
}

var methods = {
	SQLConnection: function(password) {
        config.password = password;
        ConnectSQL();
    },
    InsertData: async function(DeviceID, DeviceData, StoreDate){
        var command = "INSERT INTO RR_interval (DeviceID, DeviceData, StoreDate) VALUES ('" + DeviceID + "', " + DeviceData + ", '" + StoreDate + "')"
        SQLInsertData(command);
    },
    InsertGyroscopeData: async function(DeviceID, X, Y, Z, StoreDate){
        var command = "INSERT INTO Gyroscope (DeviceID, axisX, axisY, axisZ, StoreDate) VALUES ('" + DeviceID + "', " + X + ", " + Y + ", " + Z + ", '" + StoreDate + "')"
        SQLInsertData(command);
    },
    InsertAccelerometerData: async function(DeviceID, X, Y, Z, StoreDate){
        var command = "INSERT INTO Accelerometer (DeviceID, axisX, axisY, axisZ, StoreDate) VALUES ('" + DeviceID + "', " + X + ", " + Y + ", " + Z + ", '" + StoreDate + "')"
        SQLInsertData(command);
    }
};

module.exports = methods