uniform mat4 u_Matrix;
attribute vec3 a_Position;
varying vec3 v_Position;

void main()
{
	v_Position = a_Position;
	
	//Convert the world's right-handed coordinate space to the skybox's left-handed coordinate space
	v_Position.z = -v_Position.z;	
	gl_Position = u_Matrix*vec4(a_Position, 1.0);
	
	//Set the map on the far plane
	gl_Position = gl_Position.xyww;
}