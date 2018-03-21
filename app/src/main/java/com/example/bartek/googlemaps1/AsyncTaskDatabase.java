package com.example.bartek.googlemaps1;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.bartek.googlemaps1.Database.AppDatabase;
import com.example.bartek.googlemaps1.Database.User;
import com.example.bartek.googlemaps1.Database.UserDao;
import java.util.List;

/**
 * Created by bartek on 21.03.2018.
 */


public class AsyncTaskDatabase{

    public interface AsyncResponse {
        void processFinish(User user);
    }

    public final static String TAG = "AsyncTaskDatabase";
    private Context context;
    private UserDao userDao;
    private User user;
    private List<User> users;
    public AsyncResponse delegate = null;



    public AsyncTaskDatabase(Context context){
        this.context = context;
        AppDatabase db = Room.databaseBuilder(context,
                AppDatabase.class, "database-name").fallbackToDestructiveMigration().build();
        userDao = db.userDao();
    }

    public void inserUser(final User user){
        new AsyncTask<User, Void, Void>(){

            @Override
            protected Void doInBackground(User... users) {
                userDao.insert(user);
                return null;
            }
        }.execute();
    }

    public User getUser(){
        new AsyncTask<Void, Void, User>(){
            @Override
            protected User doInBackground(Void... voids) {
                user = userDao.getUser();
                return null;
            }

            @Override
            protected void onPostExecute(User user) {
                super.onPostExecute(user);
            }
        }.execute();
        return user;
    }

    //todo

    public void getAll(){
        new AsyncTask<Void, Void, User>(){
            @Override
            protected User doInBackground(Void... voids) {
                userDao.getUser();
                return null;
            }

            @Override
            protected void onPostExecute(User user) {
                //super.onPostExecute(user);
                //callback.finish(users);
                delegate.processFinish(user);
            }
        }.execute();

    }

    private void update(final User user){
        new AsyncTask<User, Void, Void>(){
            @Override
            protected Void doInBackground(User... users) {
                userDao.update(user);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }.execute();
    }

}
