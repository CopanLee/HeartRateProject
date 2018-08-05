const SQL = require('mssql')

let pool;

const config = {
    user:'sa',
    password: '',
    server: '127.0.0.1',
    port: 7531,
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
    insertConnectHistory: async function(DeviceID, connectDate){
        var command = "INSERT INTO connectHistory (DeviceID, connectDate) VALUES ('" + DeviceID + "', '" + connectDate + "')"
        SQLInsertData(command);
    },
    insertConnectDevice: async function(DeviceID){
        var command = "INSERT INTO connectionDevice (DeviceID) VALUES ('" + DeviceID + "')"
        SQLInsertData(command);
    },
    deleteConnectDevice: async function(DeviceID){
        var command = "DELETE FROM connectionDevice WHERE DeviceID = '" + DeviceID + "'"
        SQLInsertData(command);
    },
};

module.exports = methods
