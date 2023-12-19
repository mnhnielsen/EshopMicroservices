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
