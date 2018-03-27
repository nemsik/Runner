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
        void insertUserResponse();
        void getUserResponse(User userResponse);
        void getAllResponse(List<User> usersResponse);
        void updateResponse();
        void deleteRsponse();
    }

    public final static String TAG = "AsyncTaskDatabase";
    private Context context;
    private UserDao userDao;
    private User user;
    private List<User> users;
    public AsyncResponse delegate = null;
    private int id;



    public AsyncTaskDatabase(Context context, AsyncResponse delegate){
        this.context = context;
        this.delegate = delegate;
        AppDatabase db = Room.databaseBuilder(context,
                AppDatabase.class, "database-name").fallbackToDestructiveMigration().build();
        userDao = db.userDao();
    }

    public void inserUser(final User user){
        new AsyncTask<User, Void, Void>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(User... users) {
                userDao.insert(user);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                delegate.insertUserResponse();
            }
        }.execute();
    }

    public void getUser() {
        new AsyncTask<Void, Void, User>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected User doInBackground(Void... voids) {
                return userDao.getUser();
            }

            @Override
            protected void onPostExecute(User user) {
                delegate.getUserResponse(user);
            }
        }.execute();
    }


    public void getAll(){
        new AsyncTask<Void, Void, List<User>>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected List<User> doInBackground(Void... voids) {
                return userDao.getAll();
            }

            @Override
            protected void onPostExecute(List<User> users) {
                delegate.getAllResponse(users);
            }
        }.execute();
    }

    public void update(final User user){
        new AsyncTask<User, Void, Void>(){
            @Override
            protected Void doInBackground(User... users) {
                userDao.update(user);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                delegate.updateResponse();
            }
        }.execute();
    }

    public void delete(final User user){
        new AsyncTask<User, Void, Void>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                delegate.deleteRsponse();
            }

            @Override
            protected Void doInBackground(User... users) {
                userDao.delete(user);
                return null;
            }
        }.execute();
    }

    public void getUserById(final int id){
        new AsyncTask<Integer, Void, User>(){
            @Override
            protected User doInBackground(Integer... integers) {
                return userDao.getById(id);
                //return null;
            }

            @Override
            protected void onPostExecute(User user) {
                delegate.getUserResponse(user);
            }
        }.execute();
    }

}
