  CREATE TABLE Orders
  (
      OrderId     VARCHAR(36) PRIMARY KEY,
      CustomerId  VARCHAR(36) NOT NULL,
      OrderStatus VARCHAR(20) NOT NULL
  );

  CREATE TABLE OrderProduct
  (
      Id        INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
      OrderID   VARCHAR(36) NOT NULL,
      ProductId VARCHAR(36) NOT NULL,
      Price     DOUBLE      NOT NULL,
      Quantity  INT         NOT NULL,

      FOREIGN KEY (OrderID) REFERENCES Orders (OrderId)

  );

  CREATE TABLE Customers
  (
      Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
      CustomerId      VARCHAR(36) NOT NULL,
      CustomerName    VARCHAR(50)  NOT NULL,
      CustomerAddress VARCHAR(100) NOT NULL,
      CustomerEmail   VARCHAR(50)  NOT NULL
  );

INSERT INTO orders VALUES("390601d0-46f7-4618-9a4e-86213d5d5eb1", "3dea5bb2-9f8d-4c7f-9a97-e00de86f546d", "test");
insert into orderproduct values(1, "390601d0-46f7-4618-9a4e-86213d5d5eb1", "658ff235c10dee26a72e0e29", 100, 1);