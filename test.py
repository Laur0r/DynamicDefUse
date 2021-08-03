class myNum:

    def __init__(self, a):
        self.a = a

    def add(self, b):
        return self.a + b

myvar = myNum(1)
print(myvar.add(3))
myvar.a = 5
print(myvar.add(3))