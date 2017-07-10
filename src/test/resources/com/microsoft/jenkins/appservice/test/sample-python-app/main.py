# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License. See License.txt in the project root for
# license information.

from flask import Flask
app = Flask(__name__)

@app.route('/')
def hello_world():
  return 'Hello, Python!'

if __name__ == '__main__':
  app.run()
