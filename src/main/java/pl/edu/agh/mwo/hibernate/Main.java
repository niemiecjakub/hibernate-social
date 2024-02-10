package pl.edu.agh.mwo.hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import pl.edu.agh.mwo.hibernate.Util.HibernateUtil;
import pl.edu.agh.mwo.hibernate.models.Album;
import pl.edu.agh.mwo.hibernate.models.Photo;
import pl.edu.agh.mwo.hibernate.models.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Main {

    Session session;

    public static void main(String[] args) {
        Main main = new Main();

        // tu wstaw kod aplikacji
//		User newUser = main.addUser("user1");
//		Album newAlbum = main.addAlbum(user, "cats", "ugly cars");
//		Photo newPhoto = main.addPhoto(newAlbum, "burek");

		User user = main.getUser("user1");
		Album album = main.getAlbum(user, "cats");
		Photo photo = main.getPhoto(album, "burek");
		User user2 = main.getUser("user2");

		main.likePhoto(user2, photo);
//		System.out.println(photo);

        main.printDataTree();

        main.close();
    }

    public Main() {
        session = HibernateUtil.getSessionFactory().openSession();
    }

    public String getCurrentDate() {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        return formatter.format(date);
    }


    public User addUser(String username) {
        org.hibernate.query.Query<User> query = session.createQuery("from User ", User.class);
        List<User> users = query.list();

        boolean userAlreadyExists = users.stream().anyMatch(u -> u.getUsername().equals(username));
        if (userAlreadyExists) {
            System.out.println("User with '" + username + "' username already exists");
            return null;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setJoinDate(getCurrentDate());

        Transaction transaction = session.beginTransaction();
        session.save(newUser);
        transaction.commit();

        System.out.println("User added successfully");
        return newUser;
    }

    public User getUser(String username) {
        String hql = "FROM User C WHERE C.username=:username";
        Query<User> query = session.createQuery(hql, User.class);
        query.setParameter("username", username);
        return query.uniqueResult();
    }

    public Album addAlbum(User user, String albumName, String albumDescription) {

        boolean albumAlreadyExists = user.getAlbums().stream().anyMatch(u -> u.getName().equals(albumName));
        if (albumAlreadyExists) {
            System.out.println(user.getUsername() + " already has album with '" + albumName + "' name");
            return null;
        }

        Album newAlbum = new Album();
        newAlbum.setName(albumName);
        newAlbum.setDescription(albumDescription);

        user.addAlbum(newAlbum);

        Transaction transaction = session.beginTransaction();
        session.save(newAlbum);
        transaction.commit();

        System.out.println("Album added successfully");
        return newAlbum;
    }

//	public void removeAlbum(User user, Album album){
//		user.removeAlbum(album);
//		session.save(user);
//		System.out.println("Album removed successfully");
//	}

    public Album getAlbum(User user, String albumName) {
        String hql = "SELECT a FROM User u INNER JOIN u.albums a WHERE u.username = :username AND a.name = :albumName";
        org.hibernate.query.Query<Album> query = session.createQuery(hql, Album.class);
        query.setParameter("username", user.getUsername());
        query.setParameter("albumName", albumName);
        return query.uniqueResult();
    }

    public Photo addPhoto(Album album, String photoName) {
        Photo newPhoto = new Photo();
        newPhoto.setName(photoName);
        newPhoto.setDate(getCurrentDate());

        album.addPhoto(newPhoto);

        Transaction transaction = session.beginTransaction();
        session.save(newPhoto);
        transaction.commit();
        System.out.println("Photo added successfully");
        return newPhoto;
    }

    public void removePhoto(Photo photo){
        session.delete(photo);
    }

    public Photo getPhoto(Album album, String photoName) {
        String hql = "SELECT p FROM Album a INNER JOIN a.photos p WHERE p.name = :photoName AND a.id = :albumId";
        org.hibernate.query.Query<Photo> query = session.createQuery(hql, Photo.class);
        query.setParameter("photoName", photoName);
        query.setParameter("albumId", album.getId());
        return query.uniqueResult();
    }

    public void likePhoto(User user, Photo photo) {
        boolean alreadyLiked = user.getLikedPhotos().stream().anyMatch(p -> p.getId() == photo.getId());
        if (alreadyLiked) {
            System.out.println(user.getUsername() + " already liked this photo");
            return;
        }
        user.addLikedPhoto(photo);
        photo.addLikedBy(user);

        Transaction transaction = session.beginTransaction();
        session.save(user);
        session.save(photo);
        transaction.commit();
    }

    public void dislikePhoto(User user, Photo photo){
        user.removeLikedPhoto(photo);
        photo.removeLikedBy(user);

        Transaction transaction = session.beginTransaction();
        session.save(user);
        session.save(photo);
        transaction.commit();
    }

    public void printDataTree() {
        org.hibernate.query.Query<User> query = session.createQuery("from User ", User.class);
        List<User> users = query.list();

        for (User user : users) {
            System.out.println(user);
            if (user.getAlbums().size() == 0) {
                System.out.println("\tThis user has no albums");
                continue;
            }

            if (user.getLikedPhotos().size() > 0) {
                System.out.println("\tLiked photos:");
                for (Photo photo : user.getLikedPhotos()) {
                    System.out.println("\t\t" + photo);
                }
            }

            System.out.println("\tAlbums:");
            for (Album album : user.getAlbums()) {
                System.out.println("\t\t" + album);
                if (album.getPhotos().size() == 0) {
                    System.out.println("\t\t\tThis album has no photos");
                    continue;
                }
                for (Photo photo : album.getPhotos()) {
                    System.out.println("\t\t\t\tThis" + photo + " likes: " + photo.getLikedBy().size());
                }
            }
        }
    }


    public void close() {
        session.close();
        HibernateUtil.shutdown();
    }
}
